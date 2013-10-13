package SyntaxUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import LinguaView.syntax.CCGNode;
import LinguaView.syntax.CCGTerminalNode;

/**
 * 
 * @author c.wang
 */
public class SentenceForCCGParsing extends Sentence {
	protected List<String> adds;
	public SentenceForCCGParsing(CCGNode node) {
//		goldDerivationTree = node;
//		goldDependency = CCGChart.findDependency(goldDerivationTree, null);
		source = node.source;
		adds = new ArrayList<String>();
		for (CCGTerminalNode cn: node.collectTerminalNodes()){
			words.add(cn.word());
			poss.add(cn.modPOS());
			if (cn.additionalInfo != null)
				adds.add(cn.additionalInfo);
		}
		if (adds.size() != words.size()){
			if (adds.size() != 0)
				System.err.println("Some nodes has no additional info");
			adds = null;
		}
	}
	
	public String additionalInfo(int k){
		if (adds == null)
			return null;
		if (k >= 0 && k < length())	return adds.get(k);
		else if (k < 0)				return "#BOS#";
		else 						return "#EOS#";
	}
	
	protected SentenceForCCGParsing(){
		super();
	}
	
	public static List<SentenceForCCGParsing> readSents(String filename) throws IOException{
		return readSents(filename, true, "UTF-8");
	}
	
	public static List<SentenceForCCGParsing> readSents (String filename, boolean firstLineIsSource, String encoding) throws IOException{
		List<SentenceForCCGParsing> sents = new ArrayList<SentenceForCCGParsing>();
		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(filename), encoding));
		String s = br.readLine();
		SentenceForCCGParsing sent;// = new SentenceForCCGParsing();
		while (s!=null){
//			s = br.readLine();
			if (s.trim().length() !=0){
				sent = new SentenceForCCGParsing();
				if (firstLineIsSource){
					sent.source = s;
					s = br.readLine();
				}
				String ss[] = s.split("\\s");
				for (String wp: ss){
					int x = wp.lastIndexOf('/');
					sent.words.add(wp.substring(0, x));
					sent.poss.add(wp.substring(x+1));
				}
				sents.add(sent);
			} else {
				continue;
			}
			s = br.readLine();
		}
		br.close();
		return sents;
	}
	
	public static List<SentenceForCCGParsing> readSentsInColumn(String filename, boolean firstLineIsSource, String encoding) throws IOException{
		List<SentenceForCCGParsing> sents = new ArrayList<SentenceForCCGParsing>();
		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(filename), encoding));
		String s = br.readLine();
		SentenceForCCGParsing sent = null;// = new SentenceForCCGParsing();
		while (s!=null){
			if (s.trim().length() ==0){
				if (sent != null)
					sents.add(sent);
				sent = null;
			}
			else{
				if (sent == null){
					sent = new SentenceForCCGParsing();
					if (firstLineIsSource){
						sent.source = s;
						s = br.readLine();
						continue;
					}
				}
				String ss[] = s.split("\t");
				sent.words.add(ss[0]);
				sent.poss.add(ss[1]);
			}
			s = br.readLine();
		}
		br.close();
		return sents;
	}
	@Override
	public String toString(){
		StringBuffer sb = new StringBuffer();// = new StringBuffer(source + "\n");
		if (source != null)
			sb.append(source + "\n");
		sb.append(super.toString());
		return sb.toString();
	}
	
	public String toColumnsString(){
		StringBuffer sb = new StringBuffer();// = new StringBuffer(source + "\n");
		if (source != null)
			sb.append(source + "\n");
//		StringBuffer sb = new StringBuffer(source + "\n");
		for (int i = 0; i<length(); ++i){
			sb.append(word(i) + '\t' + pos(i) + '\n');
		}
		return sb.toString();
	}
	
	String source;
	
//	public static void main(String args[])throws IOException{
//		List<CCGNode> ccc = CCGNode.readCCGFile(new File(args[0]), "UTF-8");
//		List<SentenceForCCGParsing> sents = new ArrayList<SentenceForCCGParsing>();
//		for (CCGNode cn: ccc){
//			sents.add(new SentenceForCCGParsing(cn));
//		}
//		PrintWriter pw1 = new PrintWriter(args[1] + ".col");
//		PrintWriter pw2 = new PrintWriter(args[1] + ".sla");
//		for (SentenceForCCGParsing sent: sents){
//			pw1.println(sent.toColumnsString());
//			pw2.println(sent.toString());
//		}
//		pw1.close();
//		pw2.close();
//		
//		sents = null;
//		sents = readSentsInColumn(args[1]+".col", true, "UTF-8");
//		System.out.println(sents);
//		
//		sents = null;
//		sents = readSents(args[1]+".sla");
//		System.out.println(sents);
//		
//	}
	//FOR TEST
	@SuppressWarnings("deprecation")
	public static void main(String args[]) throws IOException{
		List<CCGNode> ccc = CCGNode.readCCGFile(new File(args[0]), "UTF-8");
		PrintWriter pw = new PrintWriter(args[1]);
		for (CCGNode cn: ccc){
			SentenceForCCGParsing sent = new SentenceForCCGParsing(cn);
			pw.println(sent.toColumnsString());
		}
		pw.close();
	}
}
