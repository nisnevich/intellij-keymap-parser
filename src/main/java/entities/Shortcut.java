package entities;

public class Shortcut {
    private String firstKeystroke;
    private String secondKeystroke;

    public Shortcut(String firstKeystroke) {
        this.firstKeystroke = firstKeystroke;
    }

    public Shortcut(String firstKeystroke, String secondKeystroke) {
        this.firstKeystroke = firstKeystroke;
        this.secondKeystroke = secondKeystroke;
    }

    public String getFirstKeystroke() {
        return firstKeystroke;
    }

    public void setFirstKeystroke(String firstKeystroke) {
        this.firstKeystroke = firstKeystroke;
    }

    public String getSecondKeystroke() {
        return secondKeystroke;
    }

    public void setSecondKeystroke(String secondKeystroke) {
        this.secondKeystroke = secondKeystroke;
    }
}
