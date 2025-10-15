import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * SimpleCalculator.java
 * Runnable in Eclipse — supports mouse + keyboard:
 * Digits 0-9, decimal ".", operators + - * /,
 * Enter or "=" for equals, Backspace, Esc to clear.
 */
public class SimpleCalculator extends JFrame {

    private final JTextField display;
    private double storedValue = 0;
    private char operator = 0;         // current pending operator, 0 if none
    private boolean startNewNumber = true; // whether next digit starts a new number

    public SimpleCalculator() {
        setTitle("Simple Calculator");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(320, 440);
        setLocationRelativeTo(null);
        setLayout(null);

        display = new JTextField("0");
        display.setBounds(10, 10, 285, 60);
        display.setFont(new Font("Arial", Font.PLAIN, 26));
        display.setHorizontalAlignment(JTextField.RIGHT);
        display.setEditable(false);
        display.setFocusable(false); // we use root pane key bindings instead
        add(display);

        String[] labels = {
            "7","8","9","/",
            "4","5","6","*",
            "1","2","3","-",
            "0",".","=","+"
        };

        JPanel panel = new JPanel(new GridLayout(4, 4, 8, 8));
        panel.setBounds(10, 80, 285, 300);
        for (String lab : labels) {
            JButton b = new JButton(lab);
            b.setFont(new Font("Arial", Font.BOLD, 20));
            b.addActionListener(e -> handleInput(lab));
            panel.add(b);
        }
        add(panel);

        JButton clear = new JButton("C");
        clear.setBounds(10, 390, 285, 30);
        clear.setFont(new Font("Arial", Font.BOLD, 16));
        clear.addActionListener(e -> clearAll());
        add(clear);

        setupKeyBindings();
        setVisible(true);
    }

    // Central input handler (buttons and key actions call this)
    private void handleInput(String in) {
        if (in.matches("[0-9]")) {
            appendDigit(in);
        } else if (".".equals(in)) {
            appendDot();
        } else if (in.matches("[+\\-*/]")) {
            setOperator(in.charAt(0));
        } else if ("=".equals(in)) {
            calculate();
        }
    }

    private void appendDigit(String d) {
        if (startNewNumber) {
            display.setText(d);
            startNewNumber = false;
        } else {
            // avoid leading zeros like "000"
            if ("0".equals(display.getText())) display.setText(d);
            else display.setText(display.getText() + d);
        }
    }

    private void appendDot() {
        if (startNewNumber) {
            display.setText("0.");
            startNewNumber = false;
        } else if (!display.getText().contains(".")) {
            display.setText(display.getText() + ".");
        }
    }

    private void setOperator(char op) {
        try {
            double current = Double.parseDouble(display.getText());
            if (operator != 0 && !startNewNumber) {
                // if there's already an operator and a second operand typed, compute first
                storedValue = compute(storedValue, current, operator);
                display.setText(formatNumber(storedValue));
            } else {
                storedValue = current;
            }
            operator = op;
            startNewNumber = true;
        } catch (NumberFormatException ex) {
            display.setText("Error");
            startNewNumber = true;
            operator = 0;
        } catch (ArithmeticException ex) {
            display.setText("Error");
            startNewNumber = true;
            operator = 0;
        }
    }

    private void calculate() {
        if (operator == 0) return; // nothing to compute
        try {
            double current = Double.parseDouble(display.getText());
            double result = compute(storedValue, current, operator);
            display.setText(formatNumber(result));
            storedValue = result;   // allow chaining (press + then new number)
            operator = 0;
            startNewNumber = true;
        } catch (NumberFormatException | ArithmeticException ex) {
            display.setText("Error");
            startNewNumber = true;
            operator = 0;
        }
    }

    private double compute(double a, double b, char op) {
        switch (op) {
            case '+': return a + b;
            case '-': return a - b;
            case '*': return a * b;
            case '/':
                if (b == 0) throw new ArithmeticException("Division by zero");
                return a / b;
            default: return b;
        }
    }

    private String formatNumber(double v) {
        // remove ".0" for integer-valued results
        if (v == (long) v) return String.format("%d", (long) v);
        else return String.valueOf(v);
    }

    private void backspace() {
        if (startNewNumber) return;
        String s = display.getText();
        if (s.length() <= 1) {
            display.setText("0");
            startNewNumber = true;
        } else {
            display.setText(s.substring(0, s.length() - 1));
        }
    }

    private void clearAll() {
        display.setText("0");
        storedValue = 0;
        operator = 0;
        startNewNumber = true;
    }

    // Key bindings so keyboard works reliably (even when display is non-editable)
    private void setupKeyBindings() {
        InputMap im = getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = getRootPane().getActionMap();

        // digits 0-9 and dot
        for (char c = '0'; c <= '9'; c++) {
            final String name = "digit" + c;
            im.put(KeyStroke.getKeyStroke("typed " + c), name);
            final String s = String.valueOf(c);
            am.put(name, new AbstractAction() {
                @Override public void actionPerformed(ActionEvent e) { handleInput(s); }
            });
        }
        im.put(KeyStroke.getKeyStroke("typed ."), "dot");
        am.put("dot", new AbstractAction() { @Override public void actionPerformed(ActionEvent e) { handleInput("."); } });

        // operators
        im.put(KeyStroke.getKeyStroke("typed +"), "plus");
        am.put("plus", new AbstractAction() { @Override public void actionPerformed(ActionEvent e) { handleInput("+"); } });
        im.put(KeyStroke.getKeyStroke("typed -"), "minus");
        am.put("minus", new AbstractAction() { @Override public void actionPerformed(ActionEvent e) { handleInput("-"); } });
        im.put(KeyStroke.getKeyStroke("typed *"), "mul");
        am.put("mul", new AbstractAction() { @Override public void actionPerformed(ActionEvent e) { handleInput("*"); } });
        im.put(KeyStroke.getKeyStroke("typed /"), "div");
        am.put("div", new AbstractAction() { @Override public void actionPerformed(ActionEvent e) { handleInput("/"); } });

        // equals: Enter and '='
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "equalsEnter");
        am.put("equalsEnter", new AbstractAction() { @Override public void actionPerformed(ActionEvent e) { handleInput("="); } });
        im.put(KeyStroke.getKeyStroke("typed ="), "equalsEq");
        am.put("equalsEq", new AbstractAction() { @Override public void actionPerformed(ActionEvent e) { handleInput("="); } });

        // backspace and escape
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0), "back");
        am.put("back", new AbstractAction() { @Override public void actionPerformed(ActionEvent e) { backspace(); } });
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "clearAll");
        am.put("clearAll", new AbstractAction() { @Override public void actionPerformed(ActionEvent e) { clearAll(); } });

        // Numpad operators (optional, many keyboards map to same typed events)
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ADD, 0), "plus");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_SUBTRACT, 0), "minus");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_MULTIPLY, 0), "mul");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_DIVIDE, 0), "div");
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(SimpleCalculator::new);
    }
}
