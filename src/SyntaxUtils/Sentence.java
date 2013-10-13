package SyntaxUtils;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import LinguaView.syntax.CCGNode;

/**
 * 
 * @author c.wang
 */
public class Sentence {
	protected List<String> words;
	protected List<String> poss;
	
	public int length() { return words.size(); }
	public String word(int k) {
		if (k >= 0 && k < length())	return words.get(k);
		else if (k < 0)				return "#BOS#";
		else 							return "#EOS#";
	}

	public String pos(int k){
		if (k >= 0 && k < length())	return poss.get(k);
		else if (k < 0)				return "#BOS#";
		else 						return "#EOS#";
	}
	
	protected Sentence(){
		words = new ArrayList<String>();
		poss = new ArrayList<String>();
	}
	
	@Override
	public String toString(){
		StringBuffer sb =new StringBuffer();
		sb.append(words.get(0) + "/" + poss.get(0));
		for (int i = 1; i<words.size(); ++i)
			sb.append(" " + words.get(i)+ "/" + poss.get(i));
		return sb.toString();
	}
	
	public String dumpWord(){
		StringBuffer sb = new StringBuffer(words.get(0));
		for (int i = 1; i < length(); ++i)
			sb.append(" " + words.get(i));
		return sb.toString();
	}	

	public String dumpWordAndPos(){
		StringBuffer sb = new StringBuffer(words.get(0) + "/" + poss.get(0));
		for (int i = 1; i< length(); ++i)
			sb.append(" " + words.get(i) + '/' + poss.get(i));
		return sb.toString();
	}

	@SuppressWarnings("deprecation")
	public static void main(String args[]) throws IOException{
		if (args.length != 3){
			System.err.println("Usage: CCGFile wordFile PosFile");
			System.exit(1);
		}	
		List<CCGNode> trees = CCGNode.readCCGFile(new File(args[0]), "UTF-8");
		PrintWriter ww[] = new PrintWriter[10];
		PrintWriter pw[] = new PrintWriter[10];
		for(int i =0; i< 10; ++i){
			ww[i] = new PrintWriter(args[1] + i); 
			pw[i] = new PrintWriter(args[2] + i); 
		}
		int partialSize = trees.size() / 10 + 1;
		for (int i  = 0; i < trees.size() ; ++i){
			Sentence sent = new SentenceForCCGParsing(trees.get(i));
			ww[i/partialSize].println(sent.dumpWord());
			pw[i/partialSize].println(sent.dumpWordAndPos());
		}
		for(int i =0; i< 10; ++i){
			ww[i].close();
			pw[i].close();
		}
	}
}
