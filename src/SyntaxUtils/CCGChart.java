package SyntaxUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import jigsaw.syntax.Lexicon;
import jigsaw.util.Triple;
import LinguaView.syntax.CCGInternalNode;
import LinguaView.syntax.CCGNode;
import LinguaView.syntax.CCGTerminalNode;
import fig.basic.Pair;
import fig.basic.PriorityQueue;
/**
 * 
 * @author c.wang
 */
public class CCGChart {
	private static final int DEFAULT_BEAM_SIZE = 16;
	private static final int MAX_RE_SEARCH = 2;
	private static final int MAX_NUM_OF_SUPERTAG = 4;
	public static boolean APPROX_PRUNE = true;
	private SentenceForCCGParsing _sent;

	private Lexicon _wordLexicon; // for shift
	private Lexicon _posLexicon; // for shift
	private CCGGrammar _grammar; // for reduce
	private PerceptronClassifier _classifier;
	
	private String[] _interpretation;
//	private PatriciaTrie<String, String> _interpretation = new PatriciaTrie<String, String>(StringKeyAnalyzer.CHAR);

	private List<State> _currStates;
	private List<Action> _goldActions;
	private PriorityQueue<State> _retainedStates;
	private List<State> _historyStates;
	
	private boolean _finish = false;
	private int _reSearchCount = 0;

	private FeatureExtractor<State> _featureExtractor = new CCGFeatures();
	public int _beam = 16;// 1 for debug
	
	public CCGChart(SentenceForCCGParsing s, CCGNode goldTree, Lexicon wordLex, Lexicon posLex, String[] interpretation, CCGGrammar g, PerceptronClassifier c, int beamSize) {
		_sent = s;
		_wordLexicon = wordLex;
		_posLexicon = posLex;
		_interpretation = interpretation;
		_grammar = g;
		_classifier = c;
		_currStates = new ArrayList<CCGChart.State>();
		if (APPROX_PRUNE) {
			_retainedStates = new PriorityQueue<State>();
			_historyStates = new ArrayList<CCGChart.State>();
		}
		_beam = beamSize;
		
		if (goldTree != null)
			_goldActions = getActionSequence(goldTree);
//		if (s.goldDerivationTree != null)
//			goldActions = getActionSequence(s.goldDerivationTree, _interpretation);
	}

	/* State is designed to support Deep-First-Search. */
	private class State implements EquivState<State> {
		private Stack<CCGNode> _stack;
		private int _queueIndex; // 0-based, index of the first one in the queue
		public boolean _gold; // used for early update
		private double _score;
		Map<Action, CCGNode> _cache = new HashMap<Action, CCGNode>();
		Map<Pair<Action, Action>, CCGNode> _cacheU = new HashMap<Pair<Action, Action>, CCGNode>();
		int _step;

		CCGRule _lastRule = null;

		public State() {
			_stack = new Stack<CCGNode>();
			_queueIndex = 0;
			_gold = true;
			_step = 0;
		}

		private boolean isFinished() {
			return _queueIndex == _sent.length() && _stack.size() == 1;
		}

		/** Shallow clone **/
		@Override
		public State clone() {
			Stack<CCGNode> newStack = new Stack<CCGNode>();
			newStack.addAll(_stack);
			State s = new State(newStack, _queueIndex, _gold, _step, _score);
			s.setLastRule(_lastRule);
			return s;
		}

		private State(Stack<CCGNode> stk, int queueIndex, boolean gold,
				int step, double score) {
			this._stack = stk;
			this._queueIndex = queueIndex;
			this._gold = gold;
			this._step = step;
			this._score = score;
		}

		private CCGNode nextNodeToStack(Action action) {
			int size = _stack.size();
			if (action.isShiftAction())
				return new CCGTerminalNode(_sent.word(_queueIndex),
						_sent.pos(_queueIndex), action._tag, _sent.additionalInfo(_queueIndex), _queueIndex); // TODO
			if (action.isBinaryReduceAction()) {
				CCGNode rChild = _stack.get(size - 1);
				CCGNode lChild = _stack.get(size - 2);
//				if (action.bRule.type != CCGBinaryRule.RuleType.unknown){
					CCGInternalNode parent = CCGInternalNode.generateNewNode(action._bRule, lChild, rChild);
					return parent;
//				}else return null;
//				return parent;
			}
			if (action.isUnaryReduceAction()) {

				CCGNode child = _stack.get(size - 1);
				// CCGUnaryRule uRule = _grammar.getUnaryRule(action._ordinal);
//				if (action.uRule.type != CCGUnaryRule.RuleType.unknown){
				CCGInternalNode newNode = CCGInternalNode.generateNewNode(action._uRule, child);
				return newNode;
//				}
//				else return null;
			}
			return null;
		}

		private State setLastRule(CCGRule rule) {
			_lastRule = rule;
			return this;
		}

		/**
		 * only temporary state for scoring
		 * @param action
		 * @return
		 */
		private State preview(Action action) {
			int windows = _featureExtractor.windows();
			State pre = new State();
			pre._step = _step;
			pre._queueIndex = _queueIndex;
			pre._gold = _gold;
			int size = _stack.size();
			
			if (action.isShiftAction())
				--windows;
			else if (action.isBinaryReduceAction())
				++windows;
			
			int newSize = size < windows ? size : windows;
			for (int i = size - newSize; i < size; ++i)
				pre._stack.push(_stack.get(i));

			CCGNode cn = _cache.get(action);
			if (cn == null) {
				cn = nextNodeToStack(action); // should always do this
				if (cn == null)
					return null;
				_cache.put(action, cn);
			}

			if (action.isShiftAction() || action.isBinaryReduceAction() || action.isUnaryReduceAction())
				return pre.generateNewState(action);
			else
				return pre;
		}

		/**
		 * generate a partial state only for scoring
		 * @param actionB
		 * @param actionU
		 * @return
		 */
		private State preview(Action actionB, Action actionU) {
			if (actionU == null)
				return preview(actionB);
			if (actionB.isUnaryReduceAction() || !actionU.isUnaryReduceAction())
				throw new IllegalArgumentException("wrong argument in preview");
			State pre = preview(actionB);
			if (pre == null)
				return null;
			CCGNode cn = _cacheU
					.get(new Pair<Action, Action>(actionB, actionU));
			if (cn == null)
				cn = pre.nextNodeToStack(actionU);
			if (cn == null)
				return null;
			pre._stack.pop();
			pre._stack.push(cn);
			pre.setLastRule(actionU._uRule);

			_cacheU.put(new Pair<Action, Action>(actionB, actionU), cn);
			return pre;
		}

		public State generateNewState(Action action) {
			State newState = clone();
			newState.act(action);
			return newState;
		}

		public State generateNewState(Action actionB, Action actionU) {
			State newState = clone();
			newState.act(actionB, actionU);
			return newState;
		}

		private boolean act(Action actionB, Action actionU) {
			CCGNode top;
			if (actionU == null) {
				top = _cache.get(actionB);
				if (top == null)
					top = nextNodeToStack(actionB);
				if (top == null)
					return false;
				return act(actionB, top);
			} else {
				if (!actionU.isUnaryReduceAction())
					throw new IllegalArgumentException("second argument of act");
				CCGNode topB = _cache.get(actionB);
				if (topB == null) {
					topB = nextNodeToStack(actionB);
					if (topB == null)
						return false;
				}
				if (!act(actionB, topB))
					return false;

				top = _cacheU.get(new Pair<Action, Action>(actionB, actionU));
				if (top == null) {
					top = CCGInternalNode.generateNewNode(actionU._uRule, topB);
					if (top == null)
						return false;
				}
				return act(actionU, top);
			}
		}

		// action could be unary action or not
		private boolean act(Action action) {
			CCGNode top = null;
			if (_cache != null)
				top = _cache.get(action);
			if (top == null)
				top = nextNodeToStack(action);
			if (top == null)
				return false;
			if (_gold && !isGoldAction(action))
				_gold = false;
			return act(action, top);
		}
		
		private boolean isGoldAction(Action action){
			if(_goldActions == null)
				return false;
			if (_step >= _goldActions.size())
				return false;
			return action.equals(_goldActions.get(_step));
		}

		private boolean act(Action action, CCGNode top) {
			if (top == null)
				return false;
			if (action.isShiftAction()) {
				_stack.push(top);
				++_queueIndex;
				_lastRule = null;
			} else if (action.isBinaryReduceAction()) {
				_stack.pop();
				_stack.pop();
				_stack.push(top);
				_lastRule = action._bRule;
			} else if (action.isUnaryReduceAction()) {
				_stack.pop();
				_stack.push(top);
				_lastRule = action._uRule;
			}
			if (_cache != null)
				_cache.clear();
			if (_cacheU != null)
				_cacheU.clear();

			if (_gold && !isGoldAction(action))
				_gold = false;
			++_step;
			return true;
		}

		@SuppressWarnings("unchecked")
		public CCGParseResult collectResult() {
			CCGParseResult res = new CCGParseResult();
			res.nodes = new ArrayList<CCGNode>(_stack);
			res.dependency = new Set[_sent.length()][];
			for (CCGNode cn : _stack) {
				for (int i = cn.start(); i <= cn.end(); ++i) {
					res.dependency[i] = new Set[cn.slots[i - cn.start()].length];
					for (int j = 0; j < res.dependency[i].length; ++j)
						res.dependency[i][j] = cn.slots[i - cn.start()][j] == null ? null
								: new HashSet<Integer>(
										cn.slots[i - cn.start()][j]);
				}
			}
//			res.actionSequence = getActionSequence(_stack);
			res.isDone = isFinished();
			res.source = _sent.source;
			return res;
		}

		/**
		 * directly set the score to s
		 * @param s
		 */
		private void score(double s) {
			this._score = s;
		}

		/**
		 * get the score of a state
		 * @return
		 */
		private double score() {
			return _score;
		}
		
		public String toString(){
			StringBuffer sb = new StringBuffer();
			sb.append(_gold? "G-" : "NG-");
			sb.append(_queueIndex + ": ");
			for (CCGNode cn: _stack)
				sb.append(cn + "\n");
			return sb.toString();
		}

		/**
		 * Approximate pruning
		 * 
		 * @param another
		 * @return
		 * @author wsun
		 */
		@Override
		public boolean isEquivalent(State another) {
			if (this._stack.size() >= 3 && another._stack.size() >= 3) {
				CCGNode lastCcgNodeOfFirst = this._stack.peek();
				CCGNode last2CcgNodeOfFirst = this._stack.elementAt(this._stack.size()-2);
				CCGNode last3CcgNodeOfFirst = this._stack.elementAt(this._stack.size()-3);

				CCGNode lastCcgNodeOfSecond = another._stack.peek();
				CCGNode last2CcgNodeOfSecond = another._stack.elementAt(another._stack.size()-2);
				CCGNode last3CcgNodeOfSecond = another._stack.elementAt(another._stack.size()-3);

				if (lastCcgNodeOfFirst.end() == lastCcgNodeOfSecond.end()
						&& last2CcgNodeOfFirst.categoryToString().equals(last2CcgNodeOfSecond.categoryToString())
						&& last3CcgNodeOfFirst.categoryToString().equals(last3CcgNodeOfSecond.categoryToString())
						&& lastCcgNodeOfFirst.categoryToString().equals(lastCcgNodeOfSecond.categoryToString()))
					return true;
			}
			if (this._stack.size() >= 2 && another._stack.size() >= 2) {
				CCGNode lastCcgNodeOfFirst = this._stack.peek();
				CCGNode last2CcgNodeOfFirst = this._stack.elementAt(this._stack.size()-2);
				CCGNode lastCcgNodeOfSecond = another._stack.peek();
				CCGNode last2CcgNodeOfSecond = another._stack.elementAt(another._stack.size()-2);
				if (lastCcgNodeOfFirst.end() == lastCcgNodeOfSecond.end()
						&& last2CcgNodeOfFirst.categoryToString().equals(last2CcgNodeOfSecond.categoryToString())
//						&& lastCcgNodeOfFirst.start() == lastCcgNodeOfSecond.start() 
						&& lastCcgNodeOfFirst.categoryToString().equals(lastCcgNodeOfSecond.categoryToString()))
					return true;
			} else if (this._stack.size() == 1 && another._stack.size() == 1) {
				CCGNode lastCcgNodeOfFirst = this._stack.peek();
				CCGNode lastCcgNodeOfSecond = another._stack.peek();
				if (lastCcgNodeOfFirst.end() == lastCcgNodeOfSecond.end()
//						&& lastCcgNodeOfFirst.start() == lastCcgNodeOfSecond.start() 
						&& lastCcgNodeOfFirst.categoryToString().equals(lastCcgNodeOfSecond.categoryToString()))
					return true;				
			}
			return false;
		}
	}


	private void proceedOneStep() {
		PriorityQueue<Triple<State, Action, Action>> pq = new PriorityQueue<Triple<State, Action, Action>>();
		for (State state : _currStates) {
			List<Action> actions = new ArrayList<Action>();
			for (Action action : getShiftActions(state))
				actions.add(action);
			for (Action action : getReduceActions(state))
				actions.add(action);

			for (Action action : actions) {
				State preview = state.preview(action);
				// double score = evaluateConfiguration(state, action);
				// if (score != Double.NEGATIVE_INFINITY) {
				if (preview != null) {
					double score = evaluateState(preview);
					pq.add(new Triple<State, Action, Action>(state, action,
							null), score + state.score());
					for (Action actionU : getUnaryActions(preview)) {
						// double scoreU = evaluateConfiguration(state, action,
						// actionU);
						// if (scoreU != Double.NEGATIVE_INFINITY)
						preview = state.preview(action, actionU);
						if (preview != null) {
							double scoreU = evaluateState(preview);
							pq.add(new Triple<State, Action, Action>(state,
									action, actionU),
									score + scoreU + state.score());
						}
					}
				}
			}
		}

    /** Modified to implement diversified version of beam search. (wsun) **/
		if (pq.size() == 0) {
			// No good state is generated. That is all old states fails.
			// This case happens only while testing, since after every step, isToEarlyUpdate() is called.
			// When pq is empty. _currStates is not updated.
			if (APPROX_PRUNE && ++_reSearchCount <= MAX_RE_SEARCH) {
				if (!_retainedStates.isEmpty()) {
					_currStates = new ArrayList<State>();
					while (_retainedStates.hasNext() && _currStates.size() < _beam) {
						State retainedState = _retainedStates.next();
						if (!pruneThisState(_currStates, retainedState)) {
							_currStates.add(retainedState);
							_historyStates.add(retainedState);
						}
					}
				} else {
					_finish = true;
				}
			} else {
				_finish = true;
			}
		} else { 
			_currStates = null;
			_currStates = new ArrayList<CCGChart.State>();
	
			int beamSize = _beam > 0 ? _beam : DEFAULT_BEAM_SIZE;
			int numOfShift = 0;
			while (pq.hasNext()) { // && i < beamSize) {
				double score = pq.getPriority();
				Triple<State, Action, Action> pair = pq.next();
				if (pair.second().isShiftAction()) { 
					numOfShift ++;
					if (numOfShift >= MAX_NUM_OF_SUPERTAG)
						continue;
				}
				State predictorState = pair.first();
				// Action action = pair.second();
				State newState = predictorState.generateNewState(pair.second(), pair.third());
				newState.score(score);
				// newState.updateScore(score);
				if (!APPROX_PRUNE) {
					_currStates.add(newState);
	
					if (_currStates.size() >= beamSize) 
						break;
				} else {
					if (_currStates.size() >= beamSize || pruneThisState(_currStates, newState)) {
						if (!pruneThisState(_historyStates, newState))
							_retainedStates.add(newState, score);
					} else {
						_currStates.add(newState);
						_historyStates.add(newState);
					}
				}
			}
		}
		
		//if (isToEarlyUpdate()) System.out.print("");
		if(isFinished())
			return;
	}
	
//	public FeatureSet collectFeats(CCGParseResult pr) {
//		List<Action> actions = pr.actionSequence;
//		return collectFeats(actions);
//	}

	private FeatureSet collectFeats(State init, List<Action> actionSequence) {
		FeatureSet fs = new FeatureSet();
		for (Action act : actionSequence) {
			init.act(act);
			fs.accumulate(_featureExtractor.getFeatures(init));
		}
		return fs;
	}

	@SuppressWarnings("unused")
	private FeatureSet collectFeats(List<Action> actionSequence) {
		return collectFeats(new State(), actionSequence);
	}

	private static List<Action> getActionSequence(Collection<CCGNode> cs) {
		if (cs.isEmpty())
			return Collections.emptyList();
		List<Action> sequence = new ArrayList<Action>();
		for (CCGNode cn : cs) {
			sequence.addAll(getActionSequence(cn));
		}
		return sequence;
	}

	private static List<Action> getActionSequence(CCGNode c) {
		Stack<CCGNode> nodesStack = new Stack<CCGNode>();
		Stack<Action> actStack = new Stack<Action>();
		nodesStack.push(c);
		while (nodesStack.size() > 0) {
			CCGNode next = nodesStack.pop();
			if (next.isTerminal()) {
				String marked = ((CCGTerminalNode) next).category().toMarkedupString();
				if (marked == null)
					return null;
				actStack.push(Action.getShiftAction(marked));
			} else {
				CCGInternalNode temp = (CCGInternalNode) next;
				if (temp.prole() == 1) {
					nodesStack.push(temp.daughters()[0]);
					actStack.push(Action.getUnaryAction(new CCGUnaryRule(temp.daughters()[0].categoryToString(), 
							next.categoryToString())));
				} else {
					nodesStack.push(temp.daughters()[0]);
					nodesStack.push(temp.daughters()[1]);
					actStack.push(Action.getReduceAction(new CCGBinaryRule(temp.daughters()[0].categoryToString(), 
							temp.daughters()[1].categoryToString(), next.categoryToString(), temp.headChild)));
				}
			}
		}
		ArrayList<Action> sequence = new ArrayList<Action>();
		while (actStack.size() > 0) {
			sequence.add(actStack.pop());
		}
		return sequence;
	}


	/**
	 * 
	 * @param states
	 * @param newState
	 * @return
	 * @author wsun
	 */
	private boolean pruneThisState(List<State> states, State newState) {
		for (State inState : states) {
			if (inState.isEquivalent(newState))
				return true;
		}
		return false;
	}

	private boolean isToEarlyUpdate() { // TODO spurious ambiguity
		for (State s : _currStates)
			if (s._gold)
				return false;
		return true;
	}
	
	/**
	 * There are three cases:
	 * (1) _currStates cannot generate new states (i.e. _finish==true when pq.size()==0);
	 * (2) _currStates is empty;
	 * (3) current best state is a DONE state, i.e. a CCG tree is generated.
	 */
	private boolean isFinished(){
		if (_finish)
			return true;
		if (_currStates.size() == 0){
			_finish = true;
			return true;
		}
		State best = _currStates.get(0);
		if (best._queueIndex == _sent.length() && best._stack.size() ==1){
			_finish = true;
			return true;
		}
		return false;
	}

	private List<Action> getShiftActions(State s) {
		if (s._queueIndex >= _sent.length())
			return Collections.<Action> emptyList();
		// String next = _sent.word(s.queueIndex);
		Set<Integer> candidates = _wordLexicon.lookup(_sent.word(s._queueIndex)
				.toLowerCase());
		if (candidates == null || candidates.size() == 0)
			candidates = _posLexicon.lookup(_sent.pos(s._queueIndex));
		if (candidates == null || candidates.size() == 0){
			System.err.println("unknown words and pos");
			return Collections.emptyList();
		}
		List<Action> res = new ArrayList<Action>();
		for (int c : candidates) {
//			String cat = _wordLexicon.tag(c);
//			if (cat == null)
//				System.out.println();
			String interpret = _interpretation[c];
			res.add(Action.getShiftAction(interpret));
		}
		return res;
	}

	private List<Action> getReduceActions(State s) {
		// TODO
		// 1. get possible reduce actions by lookup the _grammar if possible
		// 2. If _grammar doesn't work, analyze the two CG categories.
		// if step 2 necessary????
		int length = s._stack.size();
		if (length < 2)
			return Collections.emptyList();
		CCGNode left = s._stack.get(length - 2);
		CCGNode right = s._stack.get(length - 1);
		String lLabel = left.categoryToString();
		String rLabel = right.categoryToString();
		Set<CCGBinaryRule> candidate = _grammar.lookup(lLabel, rLabel);
		if (candidate == null){
			CCGBinaryRule tryRule = CCGBinaryRule.tryAllRules(left.category(), right.category());
			if (tryRule == null)
				return Collections.emptyList();
			else{
				candidate = new HashSet<CCGBinaryRule>();
				candidate.add(tryRule);
			}
		}

		List<Action> actions = new ArrayList<Action>();
		// for (int i: candidate)
		// actions.add(Action.getReduceAction(i));
		for (CCGBinaryRule r : candidate)
			actions.add(Action.getReduceAction(r));
		return actions;
	}

	private List<Action> getUnaryActions(State s) {
		int length = s._stack.size();
		if (length == 0)
			return Collections.emptyList();
		CCGNode top = s._stack.get(length - 1);
		String label = top.categoryToString().toString();
		Set<CCGUnaryRule> candidate = _grammar.lookup(label);
		if (candidate == null)
			return Collections.emptyList();

		List<Action> actions = new ArrayList<Action>();
		// for (int i: candidate)
		// actions.add(Action.getUnaryAction(i));
		for (CCGUnaryRule cat : candidate)
			actions.add(Action.getUnaryAction(cat));
		return actions;
	}

	private double evaluateState(State s) {
		FeatureSet feats = _featureExtractor.getFeatures(s);
		return _classifier.score(feats);
	}

	/**
	 * Parse for training. Stop as earlier as possible, and update parameters
	 * asap. All intermediate results for parameter updating is record.
	 */
	public void parse(boolean earlyUpdate) {
		State init = new State();
		_currStates.add(init);
		while (!_finish) {
			// multiple threads here
			proceedOneStep();
			// upon consideration of dynamic programming, prune should be done
			// after all states are calculated
			
			if (earlyUpdate && isToEarlyUpdate())
				_finish = true;
		}
//		return _currStates.get(0).collectResult();
	}

	public CCGParseResult getResult(){
		return _currStates.get(0).collectResult();
	}
	
	public void updateClassifier() { // TODO compare actions
		if (_goldActions == null){
			System.err.println(_sent.source + " is ignored because no gold parse found");
			return;
		}
		State best = _currStates.get(0);
		if (best._gold)
			return;
//		CCGParseResult pr = best.collectResult();
		List<Action> decActs = getActionSequence(best._stack);
		List<Action> goldActs = _goldActions;
		//getActionSequence(best.collectResult().nodes, _interpretation);
		
		int firstE = 0;
		State init = new State();
		int end = goldActs.size()< decActs.size() ? goldActs.size() : decActs.size();
		for (; firstE < end; ++firstE) {
			if (decActs.get(firstE).equals(goldActs.get(firstE)))
				init.act(goldActs.get(firstE));
			else
				break;
		}
		
		State init2 = init.clone();
		FeatureSet feats = collectFeats(init, decActs.subList(firstE, decActs.size()));
		_classifier.minus(feats);

		feats = null;
		feats = collectFeats(init2, goldActs.subList(firstE, end));
		_classifier.plus(feats);
	}

	public static CCGParseResult goldParse(CCGNode cn){
		SentenceForCCGParsing s = new SentenceForCCGParsing(cn);
		CCGChart chart = new CCGChart(s, null, null, null, null, null, null, 1);
		List<Action> acts;
//		try{
		acts = CCGChart.getActionSequence(cn);
		if (acts == null)
			return null;
//		}catch (Exception e){
//			e.printStackTrace();
//			return null;
//		}
		State oneState = chart.new State();
		for (Action act : acts) {
			// oneState = oneState.act(act);
			if(!oneState.act(act))
				return null;
		}
		CCGParseResult pr = oneState.collectResult();
		pr.nodes.get(0).source = cn.source;
		return pr;
	}
	
	// collect the dependency from a node
//	public static Set<Integer>[][] findDependency(CCGNode cn) {
//		SentenceForCCGParsing s = new SentenceForCCGParsing(cn);
//		CCGChart chart = new CCGChart(s, null, null, null, null, null, 16);
//		List<Action> acts = CCGChart.getActionSequence(cn);
//		State oneState = chart.new State();
//		for (Action act : acts) {
//			// oneState = oneState.act(act);
//			if(!oneState.act(act))
//				return null;
//		}
//		CCGParseResult pr = oneState.collectResult();
//		return pr.dependency;
//	}

	public enum CCGFeatureType {
		/* Stack[1].category + word unigram */
		L1W_S1C,					// Last word of the stack (i.e. Queue[-1]) + cat of the last "tree" in the stack 
		L2W_S1C,					// Second last word of the stack (i.e. Queue[-2]) + cat of the last "tree" in the stack 
		L3W_S1C,					// Third last word of the stack (i.e. Queue[-3]) + cat of the last "tree" in the stack 
		N1W_S1C,					// Queue first word + cat of the last "tree" in the stack 
		N2W_S1C,					// Queue second word + cat of the last "tree" in the stack 
		//N3W_S1C,					// Queue third word + cat of the last "tree" in the stack 
		/* Stack[1].category + word bigram */
		L1W_N1W_S1C,			// Queue[1].word + Queue[-1].word + Stack[1].cat
		N1W_N2W_S1C,			// Queue[1].word + Queue[2].word + Stack[1].cat 
		//N2W_N3W_S1C,			// Queue[2].word + Queue[3].word + Stack[1].cat
		L3W_L2W_S1C,			// Queue[-3].word + Queue[-2].word + Stack[1].cat 
		L2W_L1W_S1C,			// Queue[-2].word + Queue[-1].word + Stack[1].cat 
		
		/* Stack[1].category + POS unigram */
		L1P_S1C,					// Queue[-1].pos + Stack[1].cat 
		L2P_S1C,					// Queue[-2].pos + Stack[1].cat	
		L3P_S1C,					// Queue[-3].pos + Stack[1].cat	
		N1P_S1C,					// Queue[1].pos + Stack[1].cat 
		N2P_S1C,					// Queue[2].pos + Stack[1].cat 
		//N3P_S1C,					// Queue[3].pos + Stack[1].cat 
		/* Stack[1].category + POS bigram */
		L2P_L1P_S1C,			// Queue[-2].pos + Queue[-1].pos + Stack[1].cat
		L3P_L2P_S1C,			// Queue[-3].pos + Queue[-2].pos + Stack[1].cat
		L1P_N1P_S1C,			// Queue[-1].pos + Queue[1].pos + Stack[1].cat
		N1P_N2P_S1C,			// Queue[1].pos + Queue[2].pos + Stack[1].cat
		//N2P_N3P_S1C,			// Queue[2].pos + Queue[3].pos + Stack[1].cat

		/* Feature about Stack[1] itself */
		S1W_S1C,					// Stack[1].head_word + Stack[1].cat
		S1LeW_S1C,				// Stack[1].left_most_word + Stack[1].cat
		S1RiW_S1C,				// Stack[1].right_most_word + Stack[1].cat

		S1P_S1C,					// Stack[1].head_pos + Stack[1].cat
		S1LeP_S1C,				// Stack[1].left_most_pos + Stack[1].cat
		S1RiP_S1C,				// Stack[1].right_most_pos + Stack[1].cat
		S1LeP_S1RiP_S1C,	// Stack[1].left_most_pos + Stack[1].right_most_word + Stack[1].cat

		/* Feature about stacks */
		S1W_S2W,					// Stack[2].head_word + Stack[1].head_word
		//S2W_S1W_S1C,			// Stack[2].head_word + Stack[1].head_word + Stack[1].cat
		//S3W_S2W_S1W_S1C,	// Stack[3].head_word + Stack[2].head_word + Stack[1].head_word + Stack[1].cat
		S1C_S2C,					// Stack[1].cat + Stack[2].cat
		S1C_S2C_S3C,			// Stack[2].cat + Stack[1].cat + Stack[3].cat

		S1C_S2H,					// Stack[1].cat + Stack[2].head_word
		S1H_S2C,					// Stack[1].head_word + Stack[2].cat
		S1C_S2P,					// Stack[1].cat + Stack[2].head_pos
		S1C_S2P_S3P,			// Stack[1].cat + Stack[2].head_pos + Stack[3].head_pos

		//S1C,							// Stack[1].cat
		//S2C,							// Stack[2].cat
		//S3C,							// Stack[3].cat

		DEP_W2W,					// Inactive dependencies (of the predicate-argument structure that is completed in this step)
		DEP_P2P,					// ...
		DEP_W2P,					// ...
		DEP_P2W,					// ...
		// TODO: dependencies + cat
		DEP_W2W_S1C,					// Inactive dependencies (of the predicate-argument structure that is completed in this step)
		DEP_P2P_S1C,					// ...
		DEP_W2P_S1C,					// ...
		DEP_P2W_S1C,					// ...
		
		PRED_ARG,		//full path from predicate to argument
		PRED_ANS,	//path from predicate to ancestor
		ANS_ARG,	//path from ancestor to argument
		
		PRED_ARG_NM,		// ... NM means only part of modifier exists 
		PRED_ANS_NM,	//...
		ANS_ARG_NM,		//...
		
		PRED_ARG_NF,		// NF means path with no feature
		PRED_ANS_NF,
		ANS_ARG_NF,
		
		PRED_ARG_NMF,		// NMF means no feature and modifier is removed
		PRED_ANS_NMF,
		ANS_ARG_NMF,
		
		//RULE_AND_WORD,		// Production-like rule + word, i.e. two categories + two head words
		//S1H_S1C					 // delete 
		//
		/* Features maybe important for supertagging */
		//L1C,							// Queue[-1].stag
		//L2C,							// Queue[-2].stag
		//L3C,							// Queue[-3].stag

		/**add by cwang on June.24th**/
		/* Stack[1].category + additionalInfo unigram */
		L1A_S1C,					// Queue[-1].additionalInfo + Stack[1].cat 
		L2A_S1C,					// Queue[-2].add + Stack[1].cat	
		L3A_S1C,					// Queue[-3].add + Stack[1].cat	
		N1A_S1C,					// Queue[1].add + Stack[1].cat 
		N2A_S1C,					// Queue[2].add + Stack[1].cat 
		/* sparse? Stack[1].category + additionalInfo bigram */
		L2A_L1A_S1C,			// Queue[-2].add + Queue[-1].add + Stack[1].cat
		L3A_L2A_S1C,			// Queue[-3].add + Queue[-2].add + Stack[1].cat
		L1A_N1A_S1C,			// Queue[-1].add + Queue[1].add + Stack[1].cat
		N1A_N2A_S1C,			// Queue[1].add + Queue[2].add + Stack[1].cat
		//N2P_N3P_S1C,			// Queue[2].add + Queue[3].add + Stack[1].cat
		S1A_S1C,					// Stack[1].head_add + Stack[1].cat
		S1LeA_S1C,				// Stack[1].left_most_add + Stack[1].cat
		S1RiA_S1C,				// Stack[1].right_most_add + Stack[1].cat
		S1C_S2A,					// Stack[1].cat + Stack[2].head_add
		S1C_S2A_S3A;			// Stack[1].cat + Stack[2].head_add + Stack[3].head_add
		/**!**/
		
		String combineTwo(Object f, Object s) { 
			return this + "=" + f + "_" + s; 
		}
		
		String combineThree(Object f, Object s, Object t) { 
			return this + "=" + f + "_" + s + "_" + t; 
		}
		
		String combineFour(Object f, Object s, Object t, Object fo) { 
			return this + "=" + f + "_" + s + "_" + t + "_" + fo; 
		}
	}

	private class CCGFeatures implements FeatureExtractor<State> {
		@Override
		public int windows() {
			return 3;
		}

		private final String SEP = "-";

		@Override
		public FeatureSet getFeatures(State s) {
			FeatureSet feats = new FeatureSet();
			State preview = s.clone();
			// State preview = instance.first().preview(instance.second(),
			// instance.third());
			
			/*top 3 node on the stack*/
			CCGNode top[] = new CCGNode[3];
			top[0] = preview._stack.pop();
			top[1] = preview._stack.empty() ? null : preview._stack.pop();
			top[2] = preview._stack.empty() ? null : preview._stack.pop();

			/*top 3 cat on the stack*/
			String top1Cat = top[0].categoryToString();
			String top2Cat = top[1] == null ? "#BOS#" : top[1].categoryToString();
			String top3Cat = top[2] == null ? "#BOS#" : top[2].categoryToString();

			/*last three words and poss*/
			int index = preview._queueIndex;
			String nextWord[] = new String[3];
			String lastWord[] = new String[3];
			String nextPos[] = new String[3];
			String lastPos[] = new String[3];
			for (int i = 0; i < 3; ++i) nextWord[i] = _sent.word(index + i);
			for (int i = 0; i < 3; ++i) lastWord[i] = _sent.word(index - 1 - i);
			for (int i = 0; i < 3; ++i) nextPos[i] = _sent.pos(index + i);
			for (int i = 0; i < 3; ++i) lastPos[i] = _sent.pos(index - 1 - i);
			

			/*last three terminals*/
//			CCGTerminalNode[] lastTerm = new CCGTerminalNode[3];
//			int i = 0;
//			int j = top[i].end();
//			for (int k = 0; k < 3; ++k) {
//				if (top[i] == null)
//					break;
//				lastTerm[k] = top[i].getTerminalNode(j);
//				if (j == top[i].start())
//					i++;
//				j--;
//			}
//			String last1Cat = lastTerm[0].categoryToString();
//			String last2Cat = lastTerm[1] != null ? lastTerm[1].categoryToString() : "#BOS#";
//			String last3Cat = lastTerm[2] != null ? lastTerm[2].categoryToString() : "#BOS#";

			String s1lw = top[0].getLeftmostTerm().word();
			String s1rw = top[0].getRightmostTerm().word();
			String s1lp = top[0].getLeftmostTerm().modPOS();
			String s1rp = top[0].getRightmostTerm().modPOS();
			int s1len = top[0].end() - top[0].start() + 1;
//			CCGRule rule = preview.lastRule;
//			if(rule != null){ // should use the children of the two words
//				if (rule instanceof CCGBinaryRule)
//					feats.put(CCGFeatureType.RULE_AND_WORD.ordinal() + SEP + rule + SEP + ((CCGInternalNode)top[0]).daughters()[0].headTerm().word() +SEP+ ((CCGInternalNode)top[0]).daughters()[1].headTerm().word());
//				if (rule instanceof CCGUnaryRule)
//					feats.put(CCGFeatureType.RULE_AND_WORD.ordinal() + SEP + rule + SEP + lastWord[0]);
//			}
			
			CCGTerminalNode headTerm1 = top[0].headTerm();
			CCGTerminalNode headTerm2 = top[1] == null ? null: top[1].headTerm();
			CCGTerminalNode headTerm3 = top[2] == null ? null :top[2].headTerm();
			String headWord1 = headTerm1.word();
			String headWord2 = headTerm2 == null? "#BOS#" : headTerm2.word();
//			String headWord3 = headTerm3 == null? "#BOS#" : headTerm3.word();
			String headPos1 = headTerm1.modPOS();
			String headPos2 = headTerm2 == null? "#BOS#" : headTerm2.modPOS();
			String headPos3 = headTerm3 == null? "#BOS#" : headTerm3.modPOS();
//			String headPos3 = headTerm3 == null? "#BOS#" : headTerm3.word();
//			feats.put(CCGFeatureType.L1C.ordinal() + SEP + last1Cat);
//			if (last2Cat!=null) feats.put(CCGFeatureType.L2C.ordinal() + SEP + last2Cat);
//			if (last3Cat!=null) feats.put(CCGFeatureType.L3C.ordinal() + SEP+ last3Cat);
							
			
//			feats.put(CCGFeatureType.S1C.ordinal() + SEP + top1Cat);
//			feats.put(CCGFeatureType.S2C.ordinal() + SEP + top2Cat);
//			feats.put(CCGFeatureType.S3C.ordinal() + SEP + top3Cat);

			/* Stack[1].category + word unigram/bigram */
			feats.put(CCGFeatureType.N1W_S1C.combineTwo(nextWord[0], top1Cat));
			feats.put(CCGFeatureType.N2W_S1C.combineTwo(nextWord[1], top1Cat));
			feats.put(CCGFeatureType.L1W_S1C.combineTwo(lastWord[0], top1Cat));
			feats.put(CCGFeatureType.L2W_S1C.combineTwo(lastWord[1], top1Cat));
			feats.put(CCGFeatureType.L3W_S1C.combineTwo(lastWord[2], top1Cat));

			feats.put(CCGFeatureType.L1W_N1W_S1C.combineThree(lastWord[0], nextWord[0], top1Cat));
			feats.put(CCGFeatureType.N1W_N2W_S1C.combineThree(nextWord[0], nextWord[1], top1Cat));
			feats.put(CCGFeatureType.L3W_L2W_S1C.combineThree(lastWord[2], lastWord[1], top1Cat));
			feats.put(CCGFeatureType.L2W_L1W_S1C.combineThree(lastWord[1], lastWord[0], top1Cat));
			
			/* Stack[1].category + POS unigram/bigram */
			feats.put(CCGFeatureType.N1P_S1C.combineTwo(nextPos[0], top1Cat));
			feats.put(CCGFeatureType.N2P_S1C.combineTwo(nextPos[1], top1Cat));
			feats.put(CCGFeatureType.L1P_S1C.combineTwo(lastPos[0], top1Cat));
			feats.put(CCGFeatureType.L2P_S1C.combineTwo(lastPos[1], top1Cat));
			feats.put(CCGFeatureType.L3P_S1C.combineTwo(lastPos[2], top1Cat));
			
			feats.put(CCGFeatureType.L1P_N1P_S1C.combineThree(lastPos[0], nextPos[0], top1Cat));
			feats.put(CCGFeatureType.N1P_N2P_S1C.combineThree(nextPos[0], nextPos[1], top1Cat));
			feats.put(CCGFeatureType.L3P_L2P_S1C.combineThree(lastPos[2], lastPos[1], top1Cat));
			feats.put(CCGFeatureType.L2P_L1P_S1C.combineThree(lastPos[1], lastPos[0], top1Cat));
			
//			feats.put(CCGFeatureType.N3W_S1C.combineTwo(nextWord[2], top1Cat));
//			feats.put(CCGFeatureType.N2W_N3W_S1C.combineThree(nextWord[1], nextWord[2], top1Cat));
//			feats.put(CCGFeatureType.N3P_S1C.combineTwo(nextPos[2], top1Cat));
//			feats.put(CCGFeatureType.N2P_N3P_S1C.combineThree(nextPos[1], nextPos[2], top1Cat));
			
//			feats.put(CCGFeatureType.S1H_S1C.combineThree(headTerm1.word() + SEP + top[0].categoryToString());
			feats.put(CCGFeatureType.S1W_S1C.combineTwo(headWord1, top1Cat));
			feats.put(CCGFeatureType.S1P_S1C.combineTwo(headPos1, top1Cat));
			
			if (s1len > 1) {
				feats.put(CCGFeatureType.S1LeW_S1C.combineTwo(s1lw, top1Cat));
				feats.put(CCGFeatureType.S1RiW_S1C.combineTwo(s1rw, top1Cat));
				feats.put(CCGFeatureType.S1LeP_S1C.combineTwo(s1lp, top1Cat));
				feats.put(CCGFeatureType.S1RiP_S1C.combineTwo(s1rp, top1Cat));
				feats.put(CCGFeatureType.S1LeP_S1RiP_S1C.combineThree(s1lp, s1rp, top1Cat));
			}

			feats.put(CCGFeatureType.S1W_S2W.combineTwo(headWord1, headWord2));
			feats.put(CCGFeatureType.S1C_S2C.combineTwo(top1Cat, top2Cat));
			feats.put(CCGFeatureType.S1C_S2C_S3C.combineThree(top1Cat, top2Cat, top3Cat));

			feats.put(CCGFeatureType.S1C_S2H.combineTwo(top1Cat, headWord2));
			feats.put(CCGFeatureType.S1H_S2C.combineTwo(headWord1, top2Cat));
			feats.put(CCGFeatureType.S1C_S2P.combineTwo(top1Cat, headPos2));
			feats.put(CCGFeatureType.S1C_S2P_S3P.combineThree(top1Cat, headPos2, headPos3));
			
//			feats.put(CCGFeatureType.S2W_S1W_S1C.ordinal() + SEP + headWord2 + SEP + headTerm1.word() + SEP + top1Cat);
//			feats.put(CCGFeatureType.S3W_S2W_S1W_S1C.ordinal() + SEP + headWord3 + SEP + headWord2+ SEP + headTerm1.word() + SEP + top1Cat);

			if (headTerm1.additionalInfo != null){
				String nextAdds[] = new String[3];
				String lastAdds[] = new String[3];
				String headAdd1 = headTerm1.additionalInfo;
				String headAdd2 = headTerm2 == null? "#BOS#" : headTerm2.additionalInfo;
				String headAdd3 = headTerm3 == null? "#BOS#" : headTerm3.additionalInfo;
				String s1la = top[0].getLeftmostTerm().additionalInfo;
				String s1ra = top[0].getRightmostTerm().additionalInfo;
				
				for (int i = 0; i < 3; ++i)
					nextAdds[i] = _sent.additionalInfo(index + i);
				for (int i = 0; i < 3; ++i)
					lastAdds[i] = _sent.additionalInfo(index - 1 - i);
	
				feats.put(CCGFeatureType.S1A_S1C.ordinal() + SEP + headAdd1 + SEP + top1Cat);
				feats.put(CCGFeatureType.S1LeA_S1C.ordinal() + SEP + s1la + SEP + top1Cat);
				feats.put(CCGFeatureType.S1RiA_S1C.ordinal() + SEP + s1ra + SEP + top1Cat);
				feats.put(CCGFeatureType.S1C_S2A.ordinal() + SEP + top1Cat + SEP + headAdd2);
				feats.put(CCGFeatureType.S1C_S2A_S3A.ordinal() + SEP + top1Cat + SEP + headAdd2 + SEP + headAdd3);
	
				/* Stack[1].category + addionalInfo unigram/bigram */
				feats.put(CCGFeatureType.N1A_S1C.ordinal() + SEP + nextAdds[0] + SEP + top1Cat);
				feats.put(CCGFeatureType.N2A_S1C.ordinal() + SEP + nextAdds[1] + SEP + top1Cat);
	//			feats.put(CCGFeatureType.N3A_S1C.ordinal() + SEP + nextAdds[2] + SEP + top1Cat);
				feats.put(CCGFeatureType.L1A_S1C.ordinal() + SEP + lastAdds[0] + SEP + top1Cat);
				feats.put(CCGFeatureType.L2A_S1C.ordinal() + SEP + lastAdds[1] + SEP + top1Cat);
				feats.put(CCGFeatureType.L3A_S1C.ordinal() + SEP + lastAdds[2] + SEP + top1Cat);
				feats.put(CCGFeatureType.L1A_N1A_S1C.ordinal() + SEP + lastAdds[0] + SEP + nextAdds[0] + SEP + top1Cat);
				feats.put(CCGFeatureType.N1A_N2A_S1C.ordinal() + SEP + nextAdds[0] + SEP + nextAdds[1] + SEP + top1Cat);
	//			feats.put(CCGFeatureType.N2P_N3P_S1C.ordinal() + SEP + nextAdds[1] + SEP + nextAdds[2] + SEP + top1Cat);
				feats.put(CCGFeatureType.L3A_L2A_S1C.ordinal() + SEP + lastAdds[2] + SEP + lastAdds[1] + SEP + top1Cat);
				feats.put(CCGFeatureType.L2A_L1A_S1C.ordinal() + SEP + lastAdds[1] + SEP + lastAdds[0] + SEP + top1Cat);
			}
			
			/*DEPENDENCY FEATURES*/
			if (top[0] instanceof CCGInternalNode) { // new dependency could only on the top of the stack
				CCGInternalNode tn = (CCGInternalNode) top[0];
				Set<Integer> newlyInactiveNodes = top[0].collectNewInactiveNodes();
				for (int ii : newlyInactiveNodes) {
					CCGTerminalNode predNode = tn.getTerminalNode(ii);
					for (int jj = 0; jj < top[0].slots[ii - top[0].start()].length; ++jj) {
						// sb.append(SEP + j); // XXX same as last XXX
						for (int kk : top[0].slots[ii - top[0].start()][jj]) {
							
							StringBuffer w2w = new StringBuffer(predNode.word());
							StringBuffer p2p = new StringBuffer(predNode.modPOS());
							StringBuffer w2p = new StringBuffer(predNode.word());
							StringBuffer p2w = new StringBuffer(predNode.modPOS());
							
							CCGTerminalNode argNode = top[0].getTerminalNode(kk);
							w2w.append(SEP + argNode.word());
							w2p.append(SEP + argNode.modPOS());
							feats.put(CCGFeatureType.DEP_W2W.ordinal() + SEP + w2w.toString());
							feats.put(CCGFeatureType.DEP_W2P.ordinal() + SEP + w2p.toString());
							feats.put(CCGFeatureType.DEP_W2W_S1C.ordinal() + SEP + w2w.toString() + SEP + top1Cat);
							feats.put(CCGFeatureType.DEP_W2P_S1C.ordinal() + SEP + w2p.toString() + SEP + top1Cat);
							
							p2p.append(SEP + argNode.modPOS());
							p2w.append(SEP + argNode.word());
							feats.put(CCGFeatureType.DEP_P2P.ordinal() + SEP + p2p.toString());
							feats.put(CCGFeatureType.DEP_P2W.ordinal() + SEP + p2w.toString());
							feats.put(CCGFeatureType.DEP_P2P_S1C.ordinal() + SEP + p2p.toString() + SEP + top1Cat);
							feats.put(CCGFeatureType.DEP_P2W_S1C.ordinal() + SEP + p2w.toString() + SEP + top1Cat);
							/*
							if (ii==kk)
								continue;
							Pair<Integer, List<CCGNode>> pathPair = tn.findPath(ii, kk);
							List<CCGNode> path = pathPair.getSecond();
							Integer ancestor = pathPair.getFirst();
							if (path != null) {
								String[] leftPathItems = new String[ancestor+1];
								String[] rightPathItems = new String[path.size()-ancestor];
								int path_i = 0;
								//first just get the normal ones
								StringBuffer leftPathStrBuf = new StringBuffer();//CCGFeatureType.PRED_ARG_PATH.ordinal() + "");
								StringBuffer rightPathStrBuf = new StringBuffer();//CCGFeatureType.PRED_ARG_PATH.ordinal() + "");
								StringBuffer fullPathStrBuf = new StringBuffer();
								StringBuffer tempBuffer;
								for (; path_i < ancestor; path_i ++) {// full path
									leftPathStrBuf.append(SEP + path.get(path_i).categoryToString());
									fullPathStrBuf.append(SEP + path.get(path_i).categoryToString());
								}
								leftPathStrBuf.append(SEP + path.get(ancestor).categoryToString());
								for (path_i = ancestor; path_i < path.size(); path_i++){
									rightPathStrBuf.append(SEP+ path.get(path_i).categoryToString());
									fullPathStrBuf.append(SEP + path.get(path_i).categoryToString());
								}
								Pattern p = Pattern.compile("\\[\\w+\\]");
								Matcher m;
								
								tempBuffer = new StringBuffer(CCGFeatureType.PRED_ANS.ordinal() + "");
								tempBuffer.append(leftPathStrBuf);
								feats.put(tempBuffer.toString());

								tempBuffer = new StringBuffer(CCGFeatureType.PRED_ANS_NF.ordinal() + "");
								m = p.matcher(leftPathStrBuf);
								m.replaceAll("");//remove the feature
								tempBuffer.append(leftPathStrBuf);
								feats.put(tempBuffer.toString());

								tempBuffer = new StringBuffer(CCGFeatureType.ANS_ARG.ordinal() + "");
								tempBuffer.append(rightPathStrBuf);
								feats.put(tempBuffer.toString());
								
								tempBuffer = new StringBuffer(CCGFeatureType.ANS_ARG_NF.ordinal() + "");
								m = p.matcher(rightPathStrBuf);
								m.replaceAll("");//remove the feature
								tempBuffer.append(rightPathStrBuf);
								feats.put(tempBuffer.toString());
								
								tempBuffer = new StringBuffer(CCGFeatureType.PRED_ARG.ordinal() + "");
								tempBuffer.append(fullPathStrBuf);
								feats.put(tempBuffer.toString());
								
								tempBuffer = new StringBuffer(CCGFeatureType.PRED_ARG_NF.ordinal() + "");
								m = p.matcher(fullPathStrBuf);
								m.replaceAll("");//remove the feature
								tempBuffer.append(fullPathStrBuf);
								feats.put(tempBuffer.toString());
								
								//then we try to do something to the modifier
								path_i = 0;
								//first just get the normal ones
								leftPathStrBuf = new StringBuffer();//CCGFeatureType.PRED_ARG_PATH.ordinal() + "");
								rightPathStrBuf = new StringBuffer();//CCGFeatureType.PRED_ARG_PATH.ordinal() + "");
								fullPathStrBuf = new StringBuffer();
								for (; path_i < ancestor; path_i ++) {// full path
									CategoryObject cat = path.get(path_i).category();
									if (!cat.isAdjunct()){
										leftPathStrBuf.append(SEP + path.get(path_i).category());
										fullPathStrBuf.append(SEP + path.get(path_i).category());
									}
								}
								leftPathStrBuf.append(SEP + path.get(ancestor).category());
								for (path_i = ancestor; path_i < path.size(); path_i++){
									CategoryObject cat = path.get(path_i).category();
									if (!cat.isAdjunct()){
										rightPathStrBuf.append(SEP + path.get(path_i).category());
										fullPathStrBuf.append(SEP + path.get(path_i).category());
									}
								}
								tempBuffer = new StringBuffer(CCGFeatureType.PRED_ANS_NM.ordinal() + "");
								tempBuffer.append(leftPathStrBuf);
								feats.put(tempBuffer.toString());
								
								tempBuffer = new StringBuffer(CCGFeatureType.PRED_ANS_NMF.ordinal() + "");
								m = p.matcher(leftPathStrBuf);
								m.replaceAll("");//remove the feature
								tempBuffer.append(leftPathStrBuf);
								feats.put(tempBuffer.toString());
								
								tempBuffer = new StringBuffer(CCGFeatureType.ANS_ARG_NM.ordinal() + "");
								tempBuffer.append(rightPathStrBuf);
								feats.put(tempBuffer.toString());
								
								tempBuffer = new StringBuffer(CCGFeatureType.ANS_ARG_NMF.ordinal() + "");
								m = p.matcher(rightPathStrBuf);
								m.replaceAll("");//remove the feature
								tempBuffer.append(rightPathStrBuf);
								feats.put(tempBuffer.toString());
								
								tempBuffer = new StringBuffer(CCGFeatureType.PRED_ARG_NM.ordinal() + "");
								tempBuffer.append(fullPathStrBuf);
								feats.put(tempBuffer.toString());
								
								tempBuffer = new StringBuffer(CCGFeatureType.PRED_ARG_NMF.ordinal() + "");
								m = p.matcher(fullPathStrBuf);
								m.replaceAll("");//remove the feature
								tempBuffer.append(fullPathStrBuf);
								feats.put(tempBuffer.toString());
							}*/
						}
					}
				}
			}

			return feats;
		}
	}
}
