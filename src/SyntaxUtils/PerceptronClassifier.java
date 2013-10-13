package SyntaxUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.List;

import org.ardverk.collection.Cursor;
import org.ardverk.collection.PatriciaTrie;
import org.ardverk.collection.StringKeyAnalyzer;

import fig.basic.LogInfo;
/**
 * 
 * @author c.wang
 */
public class PerceptronClassifier extends PatriciaTrie<String, Integer> {
	private static final long serialVersionUID = -8291040694796616684L;
	
	// advanced Trie
	public PerceptronClassifier(){
		super(StringKeyAnalyzer.CHAR);
	}
	public PerceptronClassifier(PerceptronClassifier ano){
		super(ano);
	}
	
	public int score(PatriciaTrie<String, Integer> feats) {
		int score = 0;
		for (Entry<String, Integer> e: feats.entrySet()){
			Integer i = get(e.getKey());
			score += i==null? 0 : e.getValue() * i;
		}
		return score;
		
	}
	public synchronized void plus(PatriciaTrie<String, Integer> another) {
		for (Entry<String, Integer> e: another.entrySet()){
			Integer score = get(e.getKey());
			score = score == null ? e.getValue() : e.getValue() + score;
			put(e.getKey(), score);
		}
	}
	
	public synchronized void minus(PatriciaTrie<String, Integer> another){
		for (Entry<String, Integer> e: another.entrySet()){
			Integer score = get(e.getKey());
			score = score == null ? - e.getValue() : score - e.getValue();
			put(e.getKey(), score);
		}
	}
	/**
	 * @author wsun
	 */
	public int score(List<String> strFeats) {
		int score = 0;
		for (String f: strFeats){
			Integer weight = get(f);
			if (weight != null)
				score += weight;
		}
		return score;
	}
	/**
	 * @author wsun
	 */	
	public void increaseOne(List<String> strFeats) {
		for (String f : strFeats){
			Integer weight = get(f);
			if (weight != null)
				put(f, weight+1);
			else 
				put(f, 1);
		}
	}
	/**
	 * @author wsun
	 */
	public void decreaseOne(List<String> strFeats){
		for (String f : strFeats){
			Integer weight = get(f);
			if (weight != null)
				put(f, weight-1);
			else 
				put(f, -1);
		}
	}
	
	public void rebuild(){
		traverse(new RebuildCursor());
	}
	
	private class RebuildCursor implements Cursor<String, Integer>{
		@Override
		public Decision select(Entry<? extends String, ? extends Integer> arg0) {
			return arg0.getValue() == 0? Decision.REMOVE : Decision.CONTINUE;
		}
	}
	
	public void dump(Writer w) throws IOException{
		rebuild();
		for (Entry<String, Integer> e: this.entrySet())
			w.write(e.getValue() + "\t" + e.getKey() + "\n");
		w.write("\n");
	}
	
	public void load(BufferedReader br) throws IOException{
		String s = null;
		s = br.readLine();
		while(s!=null && s.trim().length() > 0){
			String[] ss = s.split("\t", 2);
			int value = Integer.parseInt(ss[0].trim());
			String key = ss[1].trim();
			this.put(key, value);
			s = br.readLine();
		}
	}
	
	@Override
	public PerceptronClassifier clone(){
		PerceptronClassifier newOne = new PerceptronClassifier();
		for (Entry<String, Integer> e: entrySet())
			newOne.put(e.getKey(), e.getValue());
		return newOne;
	}
	
	public static void main(String args[]) throws IOException{
		PerceptronClassifier weight = new PerceptronClassifier();
		FeatureSet featSet = new FeatureSet();
		featSet.put("好雨知时节");
		featSet.put("当春乃发生");
		featSet.put("随风潜入夜");
		featSet.put("润物细无声");
		featSet.put("润物细无声");
		
		LogInfo.logs(featSet);
		
		LogInfo.logs(weight);
		
		LogInfo.logs(weight.score(featSet));
		weight.plus(featSet);
		LogInfo.logs(weight);
		LogInfo.logs(weight.score(featSet));		
		weight.put("润物细无声", 1);
		LogInfo.logs(weight);
		LogInfo.logs(weight.score(featSet));
		
		PatriciaTrie<String, Integer> feat = new PatriciaTrie<String, Integer>(StringKeyAnalyzer.CHAR);
		feat.put("抽烟", 1);
		feat.put("抽烟", 1);
		feat.put("喝酒", 1);
		feat.put("烫头", 1);
		
		PerceptronClassifier feat2 = new PerceptronClassifier();
		feat2.put("洗衣", 1);
		feat2.put("做饭", 1);
		
		PatriciaTrie<String, Integer> feat3 = new PatriciaTrie<String, Integer>(StringKeyAnalyzer.CHAR);
		feat3.put("做饭", 1);
		feat3.put("抽烟", 1);
		feat3.put("烫头", 1);
		feat3.put("路痴", 1);
		
//		PerceptronClassifier feat4 = feat2.clone();
//		feat2.clear();
//		System.out.println("+++" + feat4);
//		
		System.out.println("none:" + weight.score(feat2));
		weight.plus(feat2);
		System.out.println("after pending 2 score 1: " + weight.score(feat));
		System.out.println("after pending 2 score 2: " + weight.score(feat2));
		System.out.println("after pending 2 score 3: " + weight.score(feat3));
		
		weight.minus(feat);
		System.out.println("after substracting 1 score 3: " + weight.score(feat3));
		
		weight.plus(feat3);
		System.out.println(weight);
		weight.rebuild();
		System.out.println(weight);
		
		PipedOutputStream os = new PipedOutputStream();
		PipedInputStream pi = new PipedInputStream(os);

		PrintWriter pw = new PrintWriter(os);
		weight.dump(pw);
		pw.println();
		pw.println("hahaha"); //test
		pw.close();
		
		BufferedReader bt =new BufferedReader(new InputStreamReader(pi));
		PerceptronClassifier another = new PerceptronClassifier();
		another.load(bt);
		bt.close();
		System.out.println(another);
		
		os.close();
		pi.close();
		
		
	}
}
