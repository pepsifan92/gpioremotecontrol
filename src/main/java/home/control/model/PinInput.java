package home.control.model;

/**
 * Created by Michael on 19.04.2015.
 */
public class PinInput {
    private int number;
    private boolean high;
    private long timeSinceLastChange;

    public PinInput(int number, boolean high, long timeSinceLastChange) {
        this.number = number;
        this.high = high;
        this.timeSinceLastChange = timeSinceLastChange;
    }
}
