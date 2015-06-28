package home.control.model;

/**
 * Created by Michael on 19.04.2015.
 */
public class PinInput {   
	private int number = -1;
    private boolean high;
    private long timeSinceLastChange;

    public PinInput() {}
    
    public PinInput(int number, boolean high, long timeSinceLastChange) {
        this.number = number;
        this.high = high;
        this.timeSinceLastChange = timeSinceLastChange;
    }
    
    public int getNumber() {
		return number;
	}

	public void setNumber(int number) {
		this.number = number;
	}

	public boolean getIsHigh() {
		return high;
	}

	public void setIsHigh(boolean high) {
		this.high = high;
	}

	public long getTimeSinceLastChange() {
		return timeSinceLastChange;
	}

	public void setTimeSinceLastChange(long timeSinceLastChange) {
		this.timeSinceLastChange = timeSinceLastChange;
	}

}
