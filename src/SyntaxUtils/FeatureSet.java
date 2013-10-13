package SyntaxUtils;
import org.ardverk.collection.PatriciaTrie;
import org.ardverk.collection.StringKeyAnalyzer;
/**
 * 
 * @author c.wang
 */
@SuppressWarnings("serial")
public class FeatureSet extends PatriciaTrie<String, Integer> {
	public FeatureSet(){
		super(StringKeyAnalyzer.CHAR);
	}
	
	// CAUTION: This code is rather misleading.
	public void put(String feature, int value){
		Integer freq = get(feature);
		freq = freq == null? value : freq + value;
		super.put(feature, freq);
	}
	
	public void put(String feature){
		this.put(feature, 1);
	}
	
	public void accumulate(FeatureSet fs){
		for (Entry<String, Integer> e: fs.entrySet())
			put(e.getKey(), e.getValue());
	}
	
	public void substract(FeatureSet fs){
		for (Entry<String, Integer> e: fs.entrySet())
			put(e.getKey(), -e.getValue());
		
	}
}
