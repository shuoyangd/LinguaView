package LinguaView.syntax;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Writer;

import fig.basic.Indexer;
/**
 * 
 * @author c.wang
 */
public class CategoryInterpretation {
	String _plainCat;
	public String _indexedCat;
	public String _adjustedCat;
	
	public CategoryInterpretation(String plainCat){
		
	}
	
	public static void dumpCoindex(Writer w) throws IOException{
		for (CategoryInterpretation inter: _interpretation){
			w.write(inter.toString());
			w.write("\n");
		}
		w.write("\n");
	}
	@SuppressWarnings("unused")
	public static void loadCoindex(BufferedReader br) throws IOException{
		String s = br.readLine();
	}
	
	@SuppressWarnings("unused")
	public static void loadGR(BufferedReader br) throws IOException{
		String s = br.readLine();
		
	}
	
	public String toString(){
		StringBuffer sb = new StringBuffer(_plainCat);
		sb.append("\t" + _indexedCat);
		if (_adjustedCat != null)
			sb.append("\t" + _indexedCat);
		return sb.toString();
	}
	
	static Indexer<String> plainCats;
	static CategoryInterpretation[] _interpretation;
//	static List<CategoryInterpretation> _cache;
	
	public static CategoryInterpretation lookup(String plainCat){
		int index = plainCats.indexOf(plainCat);
		if (index == -1)
			return null;
		return _interpretation[index];
	}
	
	public static String lookupCoindex(String plainCat, boolean useAdjustCat){
		CategoryInterpretation interpret = lookup(plainCat);
		if(interpret == null)
			return null;
		return interpret.getCoindex(useAdjustCat);
	}
	
	public String getCoindex(boolean useAdjustCat){
		if (useAdjustCat && _adjustedCat != null)
			return _adjustedCat;
		else return _indexedCat;
	}
}
