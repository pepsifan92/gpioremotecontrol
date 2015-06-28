package home.control.model;

public class PinConfiguration{

    private Event event;
    private int number = -1;
	private String name;
    private int debounce;
    private int pwmValue;
    private boolean outputHigh;
    private long cycleDuration;
    private long uptime;
    private long downtime;
    private int startVal;
    private int endVal;
    private boolean repeat;
    private int cycles;
    private long cyclePause;
    

    public PinConfiguration() {
        //System.out.println("Default Constructor");
    }


    public PinConfiguration(int number) {
        this.number = number;
    }

    /**
     * Used for SET EVENT
     * @param event
     * @param number
     * @param outputHigh
     */
    public PinConfiguration(Event event, int number, boolean outputHigh) {
        this.event = event;
        this.number = number;
        this.outputHigh = outputHigh;
    }

    /**
     * Used for DIM Event
     * @param event
     * @param number
     * @param pwmValue
     */
    public PinConfiguration(Event event, int number, int pwmValue) {
        this.event = event;
        this.number = number;
        setPwmValue(pwmValue);
    }

    /**
     * Used for FADE and FADE_UP_DOWN Event
     * @param event Event to run
     * @param number the number of the Pi
     * @param cycleDuration How long should a cycle (one repeating) last
     * @param startVal start value for the PWM fader
     * @param endVal end value of the PWM fader
     * @param repeat defines if the effect should run only once or more times
     * @param cycles the amount of repeatings
     * @param cyclePause the pause between the repeatings in ms
     */
    public PinConfiguration(Event event, int number, long cycleDuration, int startVal, int endVal, boolean repeat, int cycles, int cyclePause) {
        this.event = event;
        this.number = number;
        this.cycleDuration = cycleDuration;
        this.startVal = startVal;
        this.endVal = endVal;
        this.repeat = repeat;
        this.cycles = cycles;
        this.cyclePause = cyclePause;
    }

    /**
     * Used for BLINK Event
     * @param event Event to run
     * @param number the number of the Pi
     * @param uptime How long state should be up (up is the value of the given pwmValue) 
     * @param downtime How long should last the down-state. PWM = 0.
     * @param pwmValue Value of the PWM for uptime
     * @param repeat defines if the effect should run only once or more times
     * @param cycles the amount of repeatings
     */
    public PinConfiguration(Event event, int number, int uptime, int downtime, int pwmValue, boolean repeat, int cycles) {
        this.event = event;
        this.number = number;
        this.uptime = uptime;
        this.downtime = downtime;
        setPwmValue(pwmValue);
        this.repeat = repeat;
        this.cycles = cycles;
    }
    
    public Event getEvent() {
        return event;
    }

    public void setEvent(Event event) {
        this.event = event;
    }

    public int getNumber() {
        return number;
    }

    public void setNumber(int number) {
        this.number = number;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getDebounce() {
        return debounce;
    }

    public void setDebounce(int debounce) {
        this.debounce = debounce;
    }

    public int getPwmValue() {
        return pwmValue;
    }

    public void setPwmValue(int pwmValue) {    	
    	if (pwmValue > 100) {
			pwmValue = 100;
		} else if (pwmValue < 0) {
			pwmValue = 0;
		}
        this.pwmValue = pwmValue;
    }

    public boolean isOutputHigh() {
        return outputHigh;
    }

    public void setOutputHigh(boolean outputHigh) {
        this.outputHigh = outputHigh;
    }

    public long getCycleDuration() {
        return cycleDuration;
    }

    public void setCycleDuration(long cycleDuration) {
        this.cycleDuration = cycleDuration;
    }

    public long getUptime() {
        return uptime;
    }

    public void setUptime(long uptime) {
        this.uptime = uptime;
    }

    public long getDowntime() {
        return downtime;
    }

    public void setDowntime(long downtime) {
        this.downtime = downtime;
    }

    public int getStartVal() {
        return startVal;
    }

    public void setStartVal(int startVal) {
    	if (startVal > 100) {
    		startVal = 100;
		} else if (startVal < 0) {
    		startVal = 0;
		}
        this.startVal = startVal;
    }

    public int getEndVal() {
        return endVal;
    }

    public void setEndVal(int endVal) {
    	if (endVal > 100) {
    		endVal = 100;
		} else if (endVal < 0) {
    		endVal = 0;
		}
        this.endVal = endVal;
    }

    public boolean isRepeat() {
        return repeat;
    }

    public void setRepeat(boolean repeat) {
        this.repeat = repeat;
    }

    public int getCycles() {
        return cycles;
    }

    public void setCycles(int cycles) {
        this.cycles = cycles;
    }

    public long getCyclePause() {
        return cyclePause;
    }

    public void setCyclePause(long cyclePause) {
        this.cyclePause = cyclePause;
    }
}
