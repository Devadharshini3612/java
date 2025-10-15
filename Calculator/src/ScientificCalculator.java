
 import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.text.DecimalFormat;

/**
 * ScientificCalculator.java
 * Fully working scientific calculator (mouse + basic keyboard).
 * Paste into Eclipse as ScientificCalculator.java and Run.
 */
public class ScientificCalculator extends JFrame implements ActionListener {

    private final JTextField display;
    private double storedValue = 0.0;     // left operand for binary ops
    private String pendingOp = "";        // "+", "-", "*", "/", "pow", "mod"
    private boolean startNewNumber = true; // true when next digit starts a new number
    private final JToggleButton modeToggle; // DEG / RAD

    public ScientificCalculator() {
        super("Scientific Calculator");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(460, 640);
        setLocationRelativeTo(null);
        setLayout(null);

        display = new JTextField("0");
        display.setFont(new Font("Consolas", Font.PLAIN, 28));
        display.setHorizontalAlignment(SwingConstants.RIGHT);
        display.setEditable(false);
        display.setBounds(10, 10, 430, 70);
        add(display);

        // Mode toggle (DEG / RAD)
        modeToggle = new JToggleButton("DEG");
        modeToggle.setBounds(10, 90, 80, 40);
        modeToggle.setFocusable(false);
        modeToggle.addActionListener(e -> modeToggle.setText(modeToggle.isSelected() ? "RAD" : "DEG"));
        modeToggle.setSelected(false); // default DEG
        add(modeToggle);

        // Build buttons grid
        JPanel panel = new JPanel(new GridLayout(8, 5, 8, 8));
        panel.setBounds(10, 140, 430, 450);

        // Button texts in desired visual order
        String[] btns = {
            "sin", "cos", "tan", "asin", "acos",
            "atan", "ln", "log", "√", "x²",
            "x³", "xʸ", "1/x", "exp", "e",
            "pi", "mod", "%", "(", ")",
            "7", "8", "9", "/", "CE",
            "4", "5", "6", "*", "±",
            "1", "2", "3", "-", "C",
            "0", ".", "←", "+", "="
        };

        for (String t : btns) {
            JButton b = new JButton(t);
            b.setFont(new Font("Arial", Font.BOLD, 16));
            b.setFocusable(false);
            b.addActionListener(this);
            panel.add(b);
        }

        add(panel);

        setupKeyBindings();
        setVisible(true);
    }

    // Central action dispatcher for all buttons
    @Override
    public void actionPerformed(ActionEvent e) {
        String cmd = e.getActionCommand();
        try {
            if (cmd.matches("\\d")) { // 0-9
                appendDigit(cmd);
            } else if (".".equals(cmd)) {
                appendDot();
            } else if ("+".equals(cmd) || "-".equals(cmd) || "*".equals(cmd) || "/".equals(cmd)
                    || "xʸ".equals(cmd) || "mod".equals(cmd)) {
                setOperator(mapOp(cmd));
            } else if ("=".equals(cmd)) {
                calculate();
            } else {
                // unary functions and controls
                switch (cmd) {
                    case "CE": clearEntry(); break;
                    case "C": clearAll(); break;
                    case "←": backspace(); break;
                    case "±": toggleSign(); break;
                    case "%": applyUnary(v -> v / 100.0); break;
                    case "√": applyUnary(Math::sqrt); break;
                    case "x²": applyUnary(v -> v * v); break;
                    case "x³": applyUnary(v -> v * v * v); break;
                    case "1/x": applyUnary(v -> v == 0 ? Double.NaN : 1.0 / v); break;
                    case "sin": applyTrig(Math::sin); break;
                    case "cos": applyTrig(Math::cos); break;
                    case "tan": applyTrig(Math::tan); break;
                    case "asin": applyInverseTrig(Math::asin); break;
                    case "acos": applyInverseTrig(Math::acos); break;
                    case "atan": applyInverseTrig(Math::atan); break;
                    case "ln": applyUnary(v -> v <= 0 ? Double.NaN : Math.log(v)); break;
                    case "log": applyUnary(v -> v <= 0 ? Double.NaN : Math.log10(v)); break;
                    case "exp": applyUnary(Math::exp); break; // e^x
                    case "e": displayValue(Math.E); break;
                    case "pi": displayValue(Math.PI); break;
                    case "mod": // handled above as binary "mod"
                        setOperator("mod");
                        break;
                    default:
                        // parentheses included in layout but not expression parser — treat "(" and ")" as no-op
                        if ("(".equals(cmd) || ")".equals(cmd)) {
                            // optional: ignore (we don't have expression parser)
                        } else {
                            // Unknown — ignore
                        }
                        break;
                }
            }
        } catch (NumberFormatException ex) {
            display.setText("Error");
            startNewNumber = true;
            pendingOp = "";
        } catch (ArithmeticException ex) {
            display.setText("Error");
            startNewNumber = true;
            pendingOp = "";
        }
    }

    // Map label to internal operator string
    private String mapOp(String label) {
        if ("xʸ".equals(label)) return "pow";
        if ("mod".equals(label)) return "mod";
        return label;
    }

    // Append digit to display
    private void appendDigit(String d) {
        if (startNewNumber) {
            display.setText(d);
            startNewNumber = false;
        } else {
            String cur = display.getText();
            if ("0".equals(cur)) display.setText(d); // avoid leading zeros
            else display.setText(cur + d);
        }
    }

    // Append decimal point
    private void appendDot() {
        if (startNewNumber) {
            display.setText("0.");
            startNewNumber = false;
            return;
        }
        String s = display.getText();
        if (!s.contains(".")) display.setText(s + ".");
    }

    // Set binary operator (handles chaining)
    private void setOperator(String op) {
        try {
            double current = parseDisplay();
            if (!pendingOp.isEmpty() && !startNewNumber) {
                // compute storedValue pendingOp current
                storedValue = compute(storedValue, current, pendingOp);
                display.setText(formatNumber(storedValue));
            } else {
                storedValue = current;
            }
            pendingOp = op;
            startNewNumber = true;
        } catch (Exception ex) {
            display.setText("Error");
            pendingOp = "";
            startNewNumber = true;
        }
    }

    // Perform calculation for "="
    private void calculate() {
        if (pendingOp.isEmpty()) return;
        try {
            double current = parseDisplay();
            double result = compute(storedValue, current, pendingOp);
            display.setText(formatNumber(result));
            storedValue = result;
            pendingOp = "";
            startNewNumber = true;
        } catch (Exception ex) {
            display.setText("Error");
            pendingOp = "";
            startNewNumber = true;
        }
    }

    // Compute binary operations
    private double compute(double a, double b, String op) {
        switch (op) {
            case "+": return a + b;
            case "-": return a - b;
            case "*": return a * b;
            case "/":
                if (b == 0) throw new ArithmeticException("Division by zero");
                return a / b;
            case "pow": return Math.pow(a, b);
            case "mod": return a % b;
            default: return b;
        }
    }

    // Unary function applying
    private void applyUnary(UnaryOp op) {
        try {
            double v = parseDisplay();
            double res = op.apply(v);
            display.setText(formatNumber(res));
            startNewNumber = true;
            storedValue = res;
            pendingOp = "";
        } catch (Exception ex) {
            display.setText("Error");
            startNewNumber = true;
            pendingOp = "";
        }
    }

    // Trig functions with DEG/RAD handling
    private void applyTrig(UnaryOp trig) {
        try {
            double v = parseDisplay();
            if (!modeToggle.isSelected()) { // DEG mode when NOT selected (we set selected -> RAD)
                v = Math.toRadians(v);
            }
            double res = trig.apply(v);
            display.setText(formatNumber(res));
            startNewNumber = true;
            storedValue = res;
            pendingOp = "";
        } catch (Exception ex) {
            display.setText("Error");
            startNewNumber = true;
            pendingOp = "";
        }
    }

    // Inverse trig: result converted to degrees when in DEG mode
    private void applyInverseTrig(UnaryOp trigInv) {
        try {
            double v = parseDisplay();
            double raw = trigInv.apply(v);
            if (!modeToggle.isSelected()) { // DEG
                raw = Math.toDegrees(raw);
            }
            display.setText(formatNumber(raw));
            startNewNumber = true;
            storedValue = raw;
            pendingOp = "";
        } catch (Exception ex) {
            display.setText("Error");
            startNewNumber = true;
            pendingOp = "";
        }
    }

    // Place a value directly into display (used for pi, e)
    private void displayValue(double v) {
        display.setText(formatNumber(v));
        startNewNumber = true;
    }

    // Clear only current entry
    private void clearEntry() {
        display.setText("0");
        startNewNumber = true;
    }

    // Clear everything
    private void clearAll() {
        display.setText("0");
        storedValue = 0.0;
        pendingOp = "";
        startNewNumber = true;
    }

    // Backspace: remove last char
    private void backspace() {
        if (startNewNumber) return;
        String s = display.getText();
        if (s.length() <= 1 || (s.length() == 2 && s.startsWith("-"))) {
            display.setText("0");
            startNewNumber = true;
        } else {
            display.setText(s.substring(0, s.length() - 1));
        }
    }

    // Toggle sign
    private void toggleSign() {
        String s = display.getText();
        if ("0".equals(s) || "Error".equals(s)) return;
        if (s.startsWith("-")) display.setText(s.substring(1));
        else display.setText("-" + s);
    }

    // Parse display into double (throws NumberFormatException if invalid)
    private double parseDisplay() {
        String s = display.getText();
        if (s == null || s.isEmpty() || "Error".equals(s)) throw new NumberFormatException();
        return Double.parseDouble(s);
    }

    // Format number nicely (drop trailing .0)
    private String formatNumber(double v) {
        if (Double.isNaN(v) || Double.isInfinite(v)) return "Error";
        DecimalFormat df = new DecimalFormat("0.##########");
        return df.format(v);
    }

    // Functional interface for unary ops
    @FunctionalInterface
    private interface UnaryOp {
        double apply(double x);
    }

    // Key bindings for digits, dot, operators, Enter, Backspace, Esc
    private void setupKeyBindings() {
        InputMap im = getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = getRootPane().getActionMap();

        // digits
        for (char c = '0'; c <= '9'; c++) {
            final String name = "digit" + c;
            im.put(KeyStroke.getKeyStroke("typed " + c), name);
            final String s = String.valueOf(c);
            am.put(name, new AbstractAction() { public void actionPerformed(ActionEvent e) { appendDigit(s); }});
        }
        // dot
        im.put(KeyStroke.getKeyStroke("typed ."), "dot");
        am.put("dot", new AbstractAction() { public void actionPerformed(ActionEvent e) { appendDot(); }});

        // operators
        im.put(KeyStroke.getKeyStroke("typed +"), "plus");
        am.put("plus", new AbstractAction() { public void actionPerformed(ActionEvent e) { setOperator("+"); }});
        im.put(KeyStroke.getKeyStroke("typed -"), "minus");
        am.put("minus", new AbstractAction() { public void actionPerformed(ActionEvent e) { setOperator("-"); }});
        im.put(KeyStroke.getKeyStroke("typed *"), "mul");
        am.put("mul", new AbstractAction() { public void actionPerformed(ActionEvent e) { setOperator("*"); }});
        im.put(KeyStroke.getKeyStroke("typed /"), "div");
        am.put("div", new AbstractAction() { public void actionPerformed(ActionEvent e) { setOperator("/"); }});

        // equals and Enter
        im.put(KeyStroke.getKeyStroke("typed ="), "equals");
        am.put("equals", new AbstractAction() { public void actionPerformed(ActionEvent e) { calculate(); }});
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "enter");
        am.put("enter", new AbstractAction() { public void actionPerformed(ActionEvent e) { calculate(); }});

        // backspace
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0), "back");
        am.put("back", new AbstractAction() { public void actionPerformed(ActionEvent e) { backspace(); }});

        // Esc => clear all
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "clear");
        am.put("clear", new AbstractAction() { public void actionPerformed(ActionEvent e) { clearAll(); }});
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ScientificCalculator::new);
    }
}
