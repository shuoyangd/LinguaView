package SyntaxUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.ardverk.collection.PatriciaTrie;
import org.ardverk.collection.StringKeyAnalyzer;

import LinguaView.syntax.CCGNode;
import LinguaView.syntax.CCGTerminalNode;
import LinguaView.syntax.CategoryObject;
import fig.basic.LogInfo;

/**
 * CCGParseResult include the CCG derivation, the dependency, and the action sequence.
 * As a main class, it could generate the gold parse and demonstrate in HTML file.
 * @author C. Wang
 */
public class CCGParseResult {
	List<CCGNode> nodes; // nodes in the stack
	public Set<Integer>[][] dependency;
//	public List<CCGChart.Action> actionSequence;
	public boolean isDone = true;
	public String source;
	
	public boolean isDone(){
		return isDone;
	}
	
	@Override
	// for debugging
	public String toString() {
		String str = "";
		str += "#{nodes in stack}=" + nodes.size() + "\n";
		str += "Actions:\n";
//		for (CCGChart.Action act : actionSequence)
//			str += "        " + act + "\n";
		
		return str;
	}
	

	
	public String toHTML(){
		StringBuffer sb = new StringBuffer();
		if (nodes.get(0).source != null) {
			String ss[] = nodes.get(0).source.split("\\s");
			sb.append("<br><h3>" + ss[0].substring(3) + "</h3>\n");
		}
		if (nodes.size() > 1)
			sb.append("<pre>Fail to complete this sentence</pre>");
		else{
			sb.append("<pre>" + nodes.get(0).toIndentedTreeString() + "</pre>\n");
		}
		
		List<CCGTerminalNode> terms = new ArrayList<CCGTerminalNode>();
		for (CCGNode result: nodes)
			terms.addAll(result.collectTerminalNodes());
		
		sb.append("<table cellpadding=\"10\">\n");

		for (int i = 0; i < dependency.length; ++i) {
			@SuppressWarnings("unused")
			Object[] colors = {"<font color=\"firebrick\">", "</font>", "<font color=\"mediumblue\">", "</font>", "<font color=\"green\">", "</font>", "<font color=\"orchid\">", "</font>" ,"<font color=\"mediumblue\">", "</font>", "<font color=\"green\">", "</font>", "<font color=\"orchid\">", "</font>"};
			sb.append("<tr>\n");
			sb.append("<td>");
			sb.append((i+1));
			sb.append("</td>\n");
			sb.append("<td>");
			sb.append(String.format("<b>%s</b>\n", terms.get(i).word()));
			sb.append("</td>\n");
			sb.append("<td>");
			sb.append(String.format("<i>%s</i>\n", terms.get(i).category().toColoredString()));
			sb.append("</td>\n");
			for (int j = 0; j < dependency[i].length; ++j) {
				sb.append("<td>");
				if (dependency[i][j] == null)
					sb.append(String.format(
							"<font color=\"FF0000\"><b>%s</b></font>", "null"));
				else {
					sb.append(String.format("<font color=\"%s\">", CategoryObject.HTMLColors[j%4]));
					List<String> heads = new ArrayList<String>();
					for (int z : dependency[i][j])
						heads.add(terms.get(z).word());
					sb.append(heads.get(0));
					for (int z = 1; z < heads.size(); ++z)
						sb.append(", " + heads.get(z));
					sb.append("</font>");
				}
				sb.append("</td>\n");
			}
			sb.append("</tr>\n");
		}
		sb.append("</table>\n");
		
		return sb.toString();
	}
	
	public static void loadMarkup(BufferedReader br) throws IOException{

		List<String> plainCache = new ArrayList<String>();
		List<String> indexedCache = new ArrayList<String>();
		
		String s = br.readLine();
		int state = 0;
		String plainCat = null;
		String indexedCat = null;
		
		int line = 0;
		while (s != null){
			if (s.equals("EOP")) break;
			line++;
			s = s.trim();
			if (s.length() == 0){
				if (state == 1)
					LogInfo.warning("Wrong Format at line" + line);
				else if(state > 0){
					plainCache.add(plainCat);
					indexedCache.add(indexedCat);
				}
				state = 0;
			}else{
				s = s.replaceAll("#.*$", "").trim();
				if (s.length() != 0){
					switch(state){
					case 0:
						plainCat = s;
						state = 1;
						break;
					case 1:
						indexedCat = s;
						state = 2;
						break;
					case 2:
						if (s.charAt(0) == '!'){
							char c = indexedCat.charAt(0);
							s.replace('!', c);
							break;
						}
					case 3:
						state = 3;
						//TODO GR;
					}
				}
			}
			s = br.readLine();
		}
		if (state == 1) // when gr is done, state==2 is also not illegal
			LogInfo.warning("Incomplete File");
		else if(state > 0){
			plainCache.add(plainCat);
			indexedCache.add(indexedCat);
		}
		PatriciaTrie<String, String> _cache = new PatriciaTrie<String, String>(StringKeyAnalyzer.CHAR);
		for (int i = 0; i< plainCache.size(); ++i){
			String pcat = plainCache.get(i);
			_cache.put(pcat.replace('(', '{').replace(')', '}'), indexedCache.get(i)); 
		}
		CCGTerminalNode.setInterpret(_cache);
	}
	
	@SuppressWarnings("deprecation")
	public static void main(String[] args) throws IOException{
		if (args.length <= 2) {
			System.out.println("Usuage: <MarkedUp-file> <CCGBank-file> <output-file>");
			System.exit(-1);
		}
//		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream("E:/data/ccg_ch/markedup.thres4.txt"), "UTF-8"));
		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(args[0]), "UTF-8"));
		loadMarkup(br);
		br.close();
//		CCGNode c = CCGNode
//				.getCCGNodeFromString("(<T S[dcl] 0 2> (<T S[dcl] 1 2> (<T S/S 0 2> (<T S/S 1 2> (<T S[dcl] 1 2> (<T S/S 1 2> (<L NP NT NT １０月 NP>) (<L (S/S)\\NP LC LC 底 (S/S)\\NP>) ) (<T S[dcl] 1 2> (<L NP NR NR 伊拉克 NP>) (<T S[dcl]\\NP 0 2> (<T (S[dcl]\\NP)/(S[dcl]\\NP) 0 1> (<L ((S[dcl]\\NP)/(S[dcl]\\NP))/NP VV VV 宣布 ((S[dcl]\\NP)/(S[dcl]\\NP))/NP>) ) (<T S[dcl]\\NP 1 2> (<L (S\\NP)/(S\\NP) AD AD 全面 (S\\NP)/(S\\NP)>) (<T S[dcl]\\NP 0 2> (<L (S[dcl]\\NP)/NP VV VV 停止 (S[dcl]\\NP)/NP>) (<T NP 1 2> (<T NP/NP 1 2> (<T PP 0 2> (<L PP/NP P P 与 PP/NP>) (<T NP 1 2> (<L NP/NP NR NR 联合国 NP/NP>) (<L NP NN NN 特委会 NP>) ) ) (<L (NP/NP)\\PP DEG DEG 的 (NP/NP)\\PP>) ) (<L NP NN NN 合作 NP>) ) ) ) ) ) ) (<L (S/S)\\S[dcl] LC LC 后 (S/S)\\S[dcl]>) ) (<L , PU PU ， ,>) ) (<T S[dcl] 1 2> (<T NP 1 2> (<L NP/NP NR NR 美 NP/NP>) (<L NP NR NR 伊 NP>) ) (<T S[dcl]\\NP 0 2> (<T (S[dcl]\\NP)/NP 0 2> (<L (S[dcl]\\NP)/NP VV VV 爆发 (S[dcl]\\NP)/NP>) (<L (S\\NP)\\(S\\NP) AS AS 了 (S\\NP)\\(S\\NP)>) ) (<T NP 1 2> (<T NP/NP 1 2> (<T NP/NP 0 2> (<L (NP/NP)/M OD OD 第三 (NP/NP)/M>) (<L M M M 次 M>) ) (<T (NP/NP)[conj] 1 2> (<L conj CC CC 也是 conj>) (<T NP/NP 1 2> (<T (NP/NP)/(NP/NP) 1 2> (<T S[dcl]\\NP 1 2> (<L (S\\NP)/(S\\NP) AD AD 最 (S\\NP)/(S\\NP)>) (<L S[dcl]\\NP VA VA 严重 S[dcl]\\NP>) ) (<L ((NP/NP)/(NP/NP))\\(S[dcl]\\NP) DEC DEC 的 ((NP/NP)/(NP/NP))\\(S[dcl]\\NP)>) ) (<T NP/NP 0 2> (<L (NP/NP)/M CD CD 一 (NP/NP)/M>) (<L M M M 次 M>) ) ) ) ) (<L NP NN NN 危机 NP>) ) ) ) ) (<L . PU PU 。 .>) )");
//		List<CCGNode> cn = CCGNode.readCCGFile(new File("E:/data/ccg_ch/dev.mod"), "UTF-8");
		List<CCGNode> cn = CCGNode.readCCGFile(new File(args[1]), "UTF-8");
//		PrintWriter pw = new PrintWriter("E:/see.html", "GBK");
		PrintWriter pw = new PrintWriter(args[2], "GBK");
		pw.println("<html><body>");
		pw.println("This file is generated by edu.pku.coli.pear.ccg.ParseResult.");
		for(CCGNode c: cn){
			CCGParseResult pr = CCGChart.goldParse(c);
			pw.println(pr.toHTML());
			pw.println();
		}
		pw.println("</body></html>");
		pw.close();
	}
	

}
