import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class Calculator extends JFrame {

    private JLabel historyLabel;
    private JLabel displayLabel;
    private String display = "0";
    private double prev = 0;
    private String op = null;
    private boolean resetNext = false;

    private static final Color BG        = new Color(0, 0, 0);
    private static final Color DISPLAY_BG= new Color(28, 28, 30);
    private static final Color NUM_BG    = new Color(44, 44, 46);
    private static final Color TOP_BG    = new Color(99, 99, 102);
    private static final Color OP_BG     = new Color(255, 159, 10);
    private static final Color ACTIVE_BG = Color.WHITE;
    private static final Color WHITE     = Color.WHITE;
    private static final Color BLACK     = Color.BLACK;
    private static final Color OP_ACTIVE = new Color(255, 159, 10);

    private final String[] OPS = {"÷", "×", "−", "+"};
    private JButton[] opButtons = new JButton[4];

    public Calculator() {
        setTitle("Calculator");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setResizable(false);
        setBackground(BG);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(BG);
        root.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));

        // ── Display ──
        JPanel displayPanel = new JPanel();
        displayPanel.setLayout(new BoxLayout(displayPanel, BoxLayout.Y_AXIS));
        displayPanel.setBackground(DISPLAY_BG);
        displayPanel.setBorder(BorderFactory.createEmptyBorder(36, 24, 16, 24));

        historyLabel = new JLabel(" ");
        historyLabel.setForeground(new Color(136, 136, 136));
        historyLabel.setFont(new Font("SansSerif", Font.PLAIN, 13));
        historyLabel.setAlignmentX(Component.RIGHT_ALIGNMENT);

        displayLabel = new JLabel("0");
        displayLabel.setForeground(WHITE);
        displayLabel.setFont(new Font("SansSerif", Font.LAYOUT_RIGHT_TO_LEFT, 56));
        displayLabel.setAlignmentX(Component.RIGHT_ALIGNMENT);

        displayPanel.add(historyLabel);
        displayPanel.add(Box.createVerticalStrut(4));
        displayPanel.add(displayLabel);

        root.add(displayPanel, BorderLayout.NORTH);

        // ── Buttons ──
        JPanel btnPanel = new JPanel(new GridBagLayout());
        btnPanel.setBackground(BG);
        btnPanel.setBorder(BorderFactory.createEmptyBorder(10, 14, 0, 14));

        String[][] rows = {
            {"C", "±", "%", "÷"},
            {"7", "8", "9", "×"},
            {"4", "5", "6", "−"},
            {"1", "2", "3", "+"},
            {"0", ".", "="}
        };

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.weighty = 1.0;

        int opIdx = 0;
        for (int r = 0; r < rows.length; r++) {
            for (int c = 0; c < rows[r].length; c++) {
                String label = rows[r][c];
                JButton btn = createButton(label);

                gbc.gridy = r;
                gbc.gridx = (r == 4 && c == 0) ? 0 : c;
                gbc.gridwidth = (r == 4 && c == 0) ? 2 : 1;
                gbc.weightx = (r == 4 && c == 0) ? 2.0 : 1.0;

                btnPanel.add(btn, gbc);

                // Track op buttons
                for (int i = 0; i < OPS.length; i++) {
                    if (label.equals(OPS[i])) opButtons[i] = btn;
                }

                btn.addActionListener(e -> handle(label, btn));
            }
        }

        root.add(btnPanel, BorderLayout.CENTER);
        setContentPane(root);
        pack();
        setMinimumSize(new Dimension(300, 520));
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private JButton createButton(String label) {
        JButton btn = new JButton(label) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
                g2.dispose();
                super.paintComponent(g);
            }
        };

        btn.setFont(new Font("SansSerif", Font.PLAIN, 24));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setOpaque(false);
        btn.setPreferredSize(new Dimension(68, 68));

        boolean isOp    = isOperator(label) || label.equals("=");
        boolean isTop   = label.equals("C") || label.equals("±") || label.equals("%");

        btn.setBackground(isOp ? OP_BG : isTop ? TOP_BG : NUM_BG);
        btn.setForeground(isTop ? BLACK : WHITE);

        // Press animation
        btn.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                btn.setFont(btn.getFont().deriveFont(20f));
            }
            public void mouseReleased(MouseEvent e) {
                btn.setFont(btn.getFont().deriveFont(24f));
            }
        });

        return btn;
    }

    private void handle(String val, JButton clicked) {
        switch (val) {
            case "C":
                display = "0"; prev = 0; op = null; resetNext = false;
                resetOpHighlight();
                break;
            case "±":
                display = String.valueOf(-Double.parseDouble(display));
                display = cleanNumber(display);
                break;
            case "%":
                display = cleanNumber(String.valueOf(Double.parseDouble(display) / 100));
                break;
            case "=":
                if (op != null) {
                    double cur = Double.parseDouble(display);
                    double result = compute(prev, cur, op);
                    display = cleanNumber(String.valueOf(result));
                    prev = 0; op = null; resetNext = true;
                }
                resetOpHighlight();
                break;
            default:
                if (isOperator(val)) {
                    prev = Double.parseDouble(display);
                    op = val;
                    resetNext = true;
                    highlightOp(val);
                } else if (val.equals(".")) {
                    if (resetNext) { display = "0"; resetNext = false; }
                    if (!display.contains(".")) display += ".";
                } else {
                    if (resetNext || display.equals("0")) { display = val; resetNext = false; }
                    else if (display.length() < 12) display += val;
                }
        }
        render();
    }

    private double compute(double a, double b, String operator) {
        switch (operator) {
            case "÷": return b != 0 ? a / b : 0;
            case "×": return a * b;
            case "−": return a - b;
            case "+": return a + b;
        }
        return b;
    }

    private String cleanNumber(String s) {
        double d = Double.parseDouble(s);
        if (d == Math.floor(d) && !Double.isInfinite(d))
            return String.valueOf((long) d);
        // Limit decimal places
        return String.valueOf(Double.parseDouble(String.format("%.10f", d)));
    }

    private boolean isOperator(String v) {
        for (String o : OPS) if (o.equals(v)) return true;
        return false;
    }

    private void highlightOp(String active) {
        for (int i = 0; i < OPS.length; i++) {
            if (opButtons[i] == null) continue;
            if (OPS[i].equals(active)) {
                opButtons[i].setBackground(ACTIVE_BG);
                opButtons[i].setForeground(OP_ACTIVE);
            } else {
                opButtons[i].setBackground(OP_BG);
                opButtons[i].setForeground(WHITE);
            }
        }
    }

    private void resetOpHighlight() {
        for (JButton b : opButtons) {
            if (b != null) { b.setBackground(OP_BG); b.setForeground(WHITE); }
        }
    }

    private void render() {
        displayLabel.setText(display);
        int len = display.length();
        int size = len > 9 ? 32 : len > 6 ? 42 : 56;
        displayLabel.setFont(new Font("SansSerif", Font.PLAIN, size));
        historyLabel.setText(op != null ? prev + " " + op : " ");
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Calculator::new);
    }
}