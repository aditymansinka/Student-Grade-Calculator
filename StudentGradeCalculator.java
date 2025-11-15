import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.io.FileWriter;
import java.io.PrintWriter;

public class StudentGradeCalculator extends JFrame {
    private JTextField nameField;
    private JTextField[] markFields;
    private JLabel totalLabel, percentLabel, gradeLabel;
    private DefaultTableModel tableModel;
    private JTable resultTable;

    public StudentGradeCalculator() {
        setTitle("Student Grade Calculator");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(820, 540);
        setLocationRelativeTo(null);
        initUI();
    }

    private void initUI() {
        // Root panel with padding
        JPanel root = new JPanel(new BorderLayout(12, 12));
        root.setBorder(new EmptyBorder(12, 12, 12, 12));
        add(root);

        // Top title
        JLabel title = new JLabel("Student Grade Calculator", SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 22));
        root.add(title, BorderLayout.NORTH);

        // Center - form and results
        JPanel center = new JPanel(new GridLayout(1, 2, 12, 0));
        root.add(center, BorderLayout.CENTER);

        // Left side - input form
        JPanel formPanel = new JPanel();
        formPanel.setLayout(new BoxLayout(formPanel, BoxLayout.Y_AXIS));
        formPanel.setBorder(BorderFactory.createTitledBorder("Student Input"));
        center.add(formPanel);

        // Name
        JPanel nameRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        nameRow.add(new JLabel("Name:"));
        nameField = new JTextField(18);
        nameRow.add(nameField);
        formPanel.add(nameRow);

        // Subjects
        String[] subjects = {"Math", "Physics", "Chemistry", "English", "Computer"};
        markFields = new JTextField[subjects.length];
        for (int i = 0; i < subjects.length; i++) {
            JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT));
            p.add(new JLabel(subjects[i] + ":"));
            markFields[i] = new JTextField(5);
            p.add(markFields[i]);
            JLabel hint = new JLabel("(0-100)");
            hint.setFont(new Font("SansSerif", Font.ITALIC, 11));
            p.add(hint);
            formPanel.add(p);
        }

        // Buttons row
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton calcBtn = new JButton("Calculate");
        JButton addBtn = new JButton("Add to Table");
        JButton clearBtn = new JButton("Clear");
        buttons.add(calcBtn);
        buttons.add(addBtn);
        buttons.add(clearBtn);
        formPanel.add(buttons);

        // Result display
        JPanel resultPanel = new JPanel();
        resultPanel.setLayout(new GridLayout(3, 1, 6, 6));
        resultPanel.setBorder(BorderFactory.createTitledBorder("Result"));
        totalLabel = new JLabel("Total: -");
        percentLabel = new JLabel("Percentage: -");
        gradeLabel = new JLabel("Grade: -");
        totalLabel.setFont(new Font("SansSerif", Font.PLAIN, 16));
        percentLabel.setFont(new Font("SansSerif", Font.PLAIN, 16));
        gradeLabel.setFont(new Font("SansSerif", Font.BOLD, 18));
        resultPanel.add(totalLabel);
        resultPanel.add(percentLabel);
        resultPanel.add(gradeLabel);
        formPanel.add(resultPanel);

        // Right side - table and controls
        JPanel tablePanel = new JPanel(new BorderLayout(8, 8));
        tablePanel.setBorder(BorderFactory.createTitledBorder("Students"));
        center.add(tablePanel);

        // Table model
        String[] cols = {"Name", "Math", "Physics", "Chemistry", "English", "Computer", "Total", "Percentage", "Grade"};
        tableModel = new DefaultTableModel(cols, 0) {
            // make table non-editable directly
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        resultTable = new JTable(tableModel);
        resultTable.setFillsViewportHeight(true);
        JScrollPane scroll = new JScrollPane(resultTable);
        tablePanel.add(scroll, BorderLayout.CENTER);

        // Bottom controls for table
        JPanel tableButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton removeBtn = new JButton("Remove Selected");
        JButton exportBtn = new JButton("Export CSV");
        tableButtons.add(removeBtn);
        tableButtons.add(exportBtn);
        tablePanel.add(tableButtons, BorderLayout.SOUTH);

        // Action listeners
        calcBtn.addActionListener(e -> calculateAndShow());
        addBtn.addActionListener(e -> {
            if (calculateAndShow()) {
                addToTable();
            }
        });
        clearBtn.addActionListener(e -> clearInputs());
        removeBtn.addActionListener(e -> removeSelectedRow());
        exportBtn.addActionListener(e -> exportCSV());

        // Double-click row to load into form
        resultTable.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int r = resultTable.getSelectedRow();
                    if (r >= 0) loadRowToForm(r);
                }
            }
        });

        // Keyboard shortcut: Enter to calculate when in form
        KeyAdapter enterKey = new KeyAdapter() {
            public void keyPressed(KeyEvent ke) {
                if (ke.getKeyCode() == KeyEvent.VK_ENTER) {
                    calculateAndShow();
                }
            }
        };
        nameField.addKeyListener(enterKey);
        for (JTextField tf : markFields) tf.addKeyListener(enterKey);
    }

    private boolean calculateAndShow() {
        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter student name.", "Input Error", JOptionPane.WARNING_MESSAGE);
            return false;
        }

        int[] marks = new int[markFields.length];
        for (int i = 0; i < markFields.length; i++) {
            String s = markFields[i].getText().trim();
            if (s.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please enter all marks (use 0 if none).", "Input Error", JOptionPane.WARNING_MESSAGE);
                return false;
            }
            try {
                int m = Integer.parseInt(s);
                if (m < 0 || m > 100) {
                    JOptionPane.showMessageDialog(this, "Marks must be between 0 and 100.", "Input Error", JOptionPane.WARNING_MESSAGE);
                    return false;
                }
                marks[i] = m;
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Please enter numeric marks only.", "Input Error", JOptionPane.WARNING_MESSAGE);
                return false;
            }
        }

        int total = 0;
        for (int m : marks) total += m;
        double percentage = (total / (marks.length * 100.0)) * 100.0;
        String grade = computeGrade(percentage);

        totalLabel.setText(String.format("Total: %d / %d", total, marks.length * 100));
        percentLabel.setText(String.format("Percentage: %.2f%%", percentage));
        gradeLabel.setText("Grade: " + grade);

        // Color code grade label
        gradeLabel.setOpaque(true);
        gradeLabel.setForeground(Color.BLACK);
        if (percentage >= 75) {
            gradeLabel.setBackground(new Color(170, 255, 170)); // light green
        } else if (percentage >= 50) {
            gradeLabel.setBackground(new Color(255, 245, 157)); // light yellow
        } else {
            gradeLabel.setBackground(new Color(255, 179, 179)); // light red
        }
        return true;
    }

    private String computeGrade(double percentage) {
        // Modify boundaries as you wish
        if (percentage >= 90) return "A+";
        if (percentage >= 80) return "A";
        if (percentage >= 70) return "B";
        if (percentage >= 60) return "C";
        if (percentage >= 50) return "D";
        return "F";
    }

    private void addToTable() {
        String name = nameField.getText().trim();
        int[] marks = new int[markFields.length];
        for (int i = 0; i < markFields.length; i++) marks[i] = Integer.parseInt(markFields[i].getText().trim());
        int total = 0;
        for (int m : marks) total += m;
        double percentage = (total / (marks.length * 100.0)) * 100.0;
        String grade = computeGrade(percentage);

        Object[] row = new Object[9];
        row[0] = name;
        for (int i = 0; i < marks.length; i++) row[1 + i] = marks[i];
        row[6] = total;
        row[7] = String.format("%.2f", percentage);
        row[8] = grade;
        tableModel.addRow(row);

        JOptionPane.showMessageDialog(this, "Entry added to table.", "Added", JOptionPane.INFORMATION_MESSAGE);
    }

    private void clearInputs() {
        nameField.setText("");
        for (JTextField tf : markFields) tf.setText("");
        totalLabel.setText("Total: -");
        percentLabel.setText("Percentage: -");
        gradeLabel.setText("Grade: -");
        gradeLabel.setBackground(null);
    }

    private void removeSelectedRow() {
        int r = resultTable.getSelectedRow();
        if (r >= 0) {
            int choice = JOptionPane.showConfirmDialog(this, "Remove selected row?", "Confirm", JOptionPane.YES_NO_OPTION);
            if (choice == JOptionPane.YES_OPTION) {
                tableModel.removeRow(r);
            }
        } else {
            JOptionPane.showMessageDialog(this, "Select a row to remove.", "No Selection", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void loadRowToForm(int r) {
        nameField.setText((String) tableModel.getValueAt(r, 0));
        for (int i = 0; i < markFields.length; i++) {
            markFields[i].setText(String.valueOf(tableModel.getValueAt(r, 1 + i)));
        }
        totalLabel.setText("Total: " + tableModel.getValueAt(r, 6));
        percentLabel.setText("Percentage: " + tableModel.getValueAt(r, 7));
        gradeLabel.setText("Grade: " + tableModel.getValueAt(r, 8));
    }

    private void exportCSV() {
        if (tableModel.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "Table is empty. Nothing to export.", "Empty", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Save CSV");
        chooser.setSelectedFile(new java.io.File("students.csv"));
        int userSelection = chooser.showSaveDialog(this);
        if (userSelection != JFileChooser.APPROVE_OPTION) return;

        java.io.File fileToSave = chooser.getSelectedFile();
        try (PrintWriter pw = new PrintWriter(new FileWriter(fileToSave))) {
            // Header
            for (int c = 0; c < tableModel.getColumnCount(); c++) {
                pw.print(tableModel.getColumnName(c));
                if (c < tableModel.getColumnCount() - 1) pw.print(",");
            }
            pw.println();

            for (int r = 0; r < tableModel.getRowCount(); r++) {
                for (int c = 0; c < tableModel.getColumnCount(); c++) {
                    Object val = tableModel.getValueAt(r, c);
                    pw.print(val != null ? val.toString() : "");
                    if (c < tableModel.getColumnCount() - 1) pw.print(",");
                }
                pw.println();
            }
            pw.flush();
            JOptionPane.showMessageDialog(this, "Exported to: " + fileToSave.getAbsolutePath(), "Export Complete", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Failed to export: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void main(String[] args) {
        // Simple look & feel
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
        catch (Exception ignored) {}

        SwingUtilities.invokeLater(() -> {
            StudentGradeCalculator app = new StudentGradeCalculator();
            app.setVisible(true);
        });
    }
}
