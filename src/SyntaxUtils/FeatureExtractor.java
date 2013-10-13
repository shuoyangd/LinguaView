package SyntaxUtils;
/**
 * 
 * @author c.wang
 */
public interface FeatureExtractor<T> {
	public abstract FeatureSet getFeatures(T instance);
	public abstract int windows();
}
