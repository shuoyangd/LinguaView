package LinguaView.syntax;

public class Edge {
	
	private int startid;
	private int endid;
	private String label;
	
	public Edge() {
		
	}
	
	public Edge(int startid, int endid) {
		this(startid, endid, "-");
	}
	
	public Edge(int startid, int endid, String label) {
		this.startid = startid;
		this.endid = endid;
		this.label = label;
	}
	
	public int getStart() {
		return startid;
	}
	
	public int getEnd() {
		return endid;
	}
	
	public String getLabel() {
		return label;
	}
	
	public int getSpanLength() {
		return getLargeIndex() - getSmallIndex();
	}
	
	public boolean isSelfLoop() {
		if(endid == startid) {
			return true;
		}
		else {
			return false;
		}
	}
	
	public int getLargeIndex() {
		return startid > endid ? startid : endid;
	}
	
	public int getSmallIndex() {
		return startid < endid ? startid : endid;
	}
	
	/**
	 * True for pointed right, False for left.
	 * @return
	 */
	public boolean getDirection() {
		if(endid > startid) {
			return true;
		}
		else {
			return false;
		}
	}
	
	public static boolean isCross(Edge e1, Edge e2) {
		if(e1.isSelfLoop() || e2.isSelfLoop()) {
			return false;
		}
		else if(e1.getLargeIndex() > e2.getSmallIndex() &&
				e2.getSmallIndex() > e1.getSmallIndex() &&
				e2.getLargeIndex() > e1.getLargeIndex()) {
			return true;
		}
		else if(e1.getLargeIndex() > e2.getLargeIndex() &&
				e2.getLargeIndex() > e1.getSmallIndex() &&
				e2.getSmallIndex() < e1.getSmallIndex()) {
			return true;
		}
		else {
			return false;
		}
	}
	
	public static boolean isSeparate(Edge e1, Edge e2) {
		if(e1.getLargeIndex() <= e2.getSmallIndex()) {
			return true;
		}
		else if(e1.getSmallIndex() >= e2.getLargeIndex()) {
			return true;
		}
		else {
			return false;
		}
	}
	
	/**
	 * Caution: "Nested" relation is not reversible
	 * isNested(e1, e2) means e1 is on the top of e2
	 * @param e1
	 * @param e2
	 * @return
	 */
	public static boolean isNested(Edge e1, Edge e2) {
		if(e1.getLargeIndex() >= e2.getLargeIndex() &&
				e1.getSmallIndex() <= e2.getSmallIndex()) {
			return true;
		}
		else {
			return false;
		}
	}
}