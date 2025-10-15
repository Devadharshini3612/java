package mini;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.List;

/*
 Enhanced HospitalAppointmentApp
 Features added:
 - Doctor role (login & dashboard)
 - Appointment search & filter for patients
 - Doctor schedule viewer with clickable slots
 - Admin overview (stats & upcoming appointments)
 - Notification simulation panel (in-memory log)
 - Persistence (save/load HospitalSystem to file)
 - Light/Dark theme toggle
 - Cosmetic improvements and better UX

 Save as: HospitalAppointmentApp_enhanced.java
 Run with Java 8+
*/

// ------------------------
// MODEL CLASSES (Serializable)
// ------------------------

abstract class User implements Serializable {
    private static final long serialVersionUID = 1L;
    private static int ID_COUNTER = 1;
    protected final int id;
    protected String username;
    protected String passwordHash;
    protected String firstName;
    protected String lastName;
    protected String email;

    public User(String username, String passwordPlain, String firstName, String lastName, String email) {
        this.id = ID_COUNTER++;
        this.username = username;
        this.passwordHash = hash(passwordPlain);
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
    }

    public static String hash(String plain) { return Integer.toHexString(Objects.hash(plain)); }
    public boolean verifyPassword(String plain) { return Objects.equals(passwordHash, hash(plain)); }

    public int getId() { return id; }
    public String getUsername() { return username; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getEmail() { return email; }

    @Override public String toString() { return String.format("%s (%s %s)", username, firstName, lastName); }
}

class Patient extends User {
    public Patient(String username, String passwordPlain, String firstName, String lastName, String email) {
        super(username, passwordPlain, firstName, lastName, email);
    }
}

class Admin extends User {
    public Admin(String username, String passwordPlain, String firstName, String lastName, String email) {
        super(username, passwordPlain, firstName, lastName, email);
    }
}

class Doctor extends User {
    private static final long serialVersionUID = 1L;
    private static int ID_COUNTER_DOC = 1;
    private final int docId;
    private String specialization;
    private List<LocalDateTime> availableSlots = new ArrayList<>();

    public Doctor(String username, String passwordPlain, String firstName, String lastName, String email, String specialization) {
        super(username, passwordPlain, firstName, lastName, email);
        this.docId = ID_COUNTER_DOC++;
        this.specialization = specialization;
        generateSlots();
    }

    private void generateSlots() {
        // Generate sample slots for next 7 days at 09:00, 11:00, 14:00, 16:00
        availableSlots.clear();
        LocalDate start = LocalDate.now();
        for (int d = 0; d < 7; d++) {
            LocalDate date = start.plusDays(d);
            availableSlots.add(LocalDateTime.of(date, LocalTime.of(9,0)));
            availableSlots.add(LocalDateTime.of(date, LocalTime.of(11,0)));
            availableSlots.add(LocalDateTime.of(date, LocalTime.of(14,0)));
            availableSlots.add(LocalDateTime.of(date, LocalTime.of(16,0)));
        }
    }

    public int getDocId() { return docId; }
    public String getName() { return firstName + " " + lastName; }
    public String getSpecialization() { return specialization; }
    public List<LocalDateTime> getAvailableSlots() { return availableSlots; }

    @Override public String toString() { return getName() + " - " + specialization; }
}

class Appointment implements Serializable {
    private static final long serialVersionUID = 1L;
    private static int ID_COUNTER = 1;
    private final int id;
    private final Patient patient;
    private final Doctor doctor;
    private LocalDateTime dateTime;
    private String reason;
    private Status status;

    enum Status { SCHEDULED, CANCELLED, COMPLETED }

    public Appointment(Patient patient, Doctor doctor, LocalDateTime dateTime, String reason) {
        this.id = ID_COUNTER++;
        this.patient = patient;
        this.doctor = doctor;
        this.dateTime = dateTime;
        this.reason = reason;
        this.status = Status.SCHEDULED;
    }

    public int getId() { return id; }
    public Patient getPatient() { return patient; }
    public Doctor getDoctor() { return doctor; }
    public LocalDateTime getDateTime() { return dateTime; }
    public String getReason() { return reason; }
    public Status getStatus() { return status; }
    public void cancel() { this.status = Status.CANCELLED; }
    public void complete() { this.status = Status.COMPLETED; }

    @Override public String toString() {
        return String.format("Appt#%d | %s with %s at %s | %s", id, patient.getUsername(), doctor.getName(), dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")), status);
    }
}

// ------------------------
// HOSPITAL SYSTEM (Serializable)
// ------------------------

class HospitalSystem implements Serializable {
    private static final long serialVersionUID = 1L;
    private List<User> users = new ArrayList<>();
    private List<Doctor> doctors = new ArrayList<>();
    private List<Appointment> appointments = new ArrayList<>();

    public HospitalSystem() { /* empty */ }

    public void seedSampleData() {
        if (!doctors.isEmpty() || !users.isEmpty()) return;
        addDoctor(new Doctor("emilysmith","docpass","Emily","Smith","emily.smith@gmail.com","Cardiology"));
        addDoctor(new Doctor("rajiv","docpass","Rajiv","Patel","rajiv.patel@gmail.com","Orthopedics"));
        addDoctor(new Doctor("sarab","docpass","Sara","Brown","sara.brown@gmail.com","Pediatrics"));
        addUser(new Patient("Dharshini","3612","Dharshini","M","rogith123@gmail.com"));
        addUser(new Admin("admin","admin123","Super","Admin","admin123@gmail.com"));
        addUser(new Patient("Rogith","1234","Rogith","M","dharshinimurali63@gmail.com"));
    }

    public synchronized void addUser(User u) { users.add(u); }
    public Optional<User> findUserByUsername(String uname) {
        return users.stream().filter(u -> u.getUsername().equalsIgnoreCase(uname)).findFirst();
    }
    public List<User> getUsers() { return Collections.unmodifiableList(users); }

    public synchronized void addDoctor(Doctor d) { doctors.add(d); users.add(d); }
    public boolean removeDoctorByDocId(int id) { return doctors.removeIf(d -> d.getDocId() == id); }
    public List<Doctor> getDoctors() { return Collections.unmodifiableList(doctors); }

    public synchronized Appointment bookAppointment(Patient p, Doctor d, LocalDateTime at, String reason) throws Exception {
        boolean conflict = appointments.stream()
                .filter(a -> a.getStatus() == Appointment.Status.SCHEDULED)
                .anyMatch(a -> a.getDoctor().getDocId() == d.getDocId() && a.getDateTime().equals(at));
        if (conflict) throw new Exception("Doctor already has an appointment at that time.");
        Appointment appt = new Appointment(p, d, at, reason);
        appointments.add(appt);
        return appt;
    }

    public List<Appointment> getAppointments() { return Collections.unmodifiableList(appointments); }
    public List<Appointment> getAppointmentsForPatient(Patient p) {
        List<Appointment> out = new ArrayList<>();
        for (Appointment a : appointments) if (a.getPatient().getId() == p.getId()) out.add(a);
        return out;
    }
    public List<Appointment> getAppointmentsForDoctor(Doctor d) {
        List<Appointment> out = new ArrayList<>();
        for (Appointment a : appointments) if (a.getDoctor().getDocId() == d.getDocId()) out.add(a);
        return out;
    }
    public Optional<Appointment> findAppointmentById(int id) { return appointments.stream().filter(a -> a.getId() == id).findFirst(); }

    // Persistence helpers
    public static void saveToFile(HospitalSystem sys, File f) throws IOException {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(f))) {
            oos.writeObject(sys);
        }
    }
    public static HospitalSystem loadFromFile(File f) throws IOException, ClassNotFoundException {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(f))) {
            return (HospitalSystem) ois.readObject();
        }
    }
}

// ------------------------
// Notification center (simple in-memory log)
// ------------------------

class NotificationCenter {
    private final List<String> messages = new ArrayList<>();
    public synchronized void notify(String msg) {
        String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        messages.add(ts + " - " + msg);
    }
    public synchronized List<String> all() { return new ArrayList<>(messages); }
}

// ------------------------
// GUI APPLICATION
// ------------------------

public class HospitalAppointmentAppEnhanced extends JFrame {

    private HospitalSystem system;
    private final File persistenceFile = new File("hospital_system.dat");
    private final NotificationCenter notifications = new NotificationCenter();

    private User loggedInUser = null;
    private CardLayout cardLayout = new CardLayout();
    private JPanel mainPanel = new JPanel(cardLayout);
    private final DateTimeFormatter dtFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private PatientPanel patientPanelRef;
    private AdminPanel adminPanelRef;
    private DoctorPanel doctorPanelRef;
    private LoginPanel loginPanelRef;

    private JButton btnLogout;

    public HospitalAppointmentAppEnhanced() {
        loadOrCreateSystem();
        initializeUI();
    }

    private void loadOrCreateSystem() {
        if (persistenceFile.exists()) {
            try {
                system = HospitalSystem.loadFromFile(persistenceFile);
            } catch (Exception e) {
                e.printStackTrace();
                system = new HospitalSystem();
                system.seedSampleData();
            }
        } else {
            system = new HospitalSystem();
            system.seedSampleData();
        }
    }

    private void saveSystem() {
        try {
            HospitalSystem.saveToFile(system, persistenceFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void initializeUI() {
        setTitle("CareConnect Enhanced");
        setSize(1100, 700);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setLocationRelativeTo(null);

        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) {
                int res = JOptionPane.showConfirmDialog(HospitalAppointmentAppEnhanced.this, "Save changes & exit?", "Exit", JOptionPane.YES_NO_CANCEL_OPTION);
                if (res == JOptionPane.YES_OPTION) {
                    saveSystem();
                    dispose();
                    System.exit(0);
                } else if (res == JOptionPane.NO_OPTION) {
                    dispose();
                    System.exit(0);
                }
            }
        });

        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBorder(new EmptyBorder(8, 12, 8, 12));
        JLabel brand = new JLabel("\uD83D\uDC89 CareConnect");
        brand.setFont(new Font("SansSerif", Font.BOLD, 22));
        topBar.add(brand, BorderLayout.WEST);

        JPanel rightControls = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton themeToggle = new JButton("Toggle Theme");
        btnLogout = new JButton("Logout"); btnLogout.setVisible(false);
        rightControls.add(themeToggle);
        rightControls.add(btnLogout);
        topBar.add(rightControls, BorderLayout.EAST);

        themeToggle.addActionListener(e -> toggleTheme());
        btnLogout.addActionListener(e -> doLogout());

        mainPanel.add(welcomePanel(), "welcome");
        mainPanel.add(loginPanel(), "login");
        mainPanel.add(patientDashboard(), "patient");
        mainPanel.add(adminDashboard(), "admin");
        mainPanel.add(doctorDashboard(), "doctor");

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(topBar, BorderLayout.NORTH);
        getContentPane().add(mainPanel, BorderLayout.CENTER);

        setLoginCallback(user -> {
            loggedInUser = user;
            btnLogout.setVisible(true);
            if (user instanceof Patient) {
                patientPanelRef.refreshData();
                cardLayout.show(mainPanel, "patient");
            } else if (user instanceof Admin) {
                adminPanelRef.refreshData();
                cardLayout.show(mainPanel, "admin");
            } else if (user instanceof Doctor) {
                doctorPanelRef.refreshData();
                cardLayout.show(mainPanel, "doctor");
            }
        });

        cardLayout.show(mainPanel, "welcome");
    }

    private void doLogout() {
        loggedInUser = null;
        btnLogout.setVisible(false);
        cardLayout.show(mainPanel, "welcome");
    }

    private void toggleTheme() {
        try {
            UIManager.LookAndFeelInfo[] lafs = UIManager.getInstalledLookAndFeels();
            boolean switched = false;
            for (UIManager.LookAndFeelInfo info : lafs) {
                if (UIManager.getLookAndFeel().getName().equals(info.getName())) continue;
                UIManager.setLookAndFeel(info.getClassName());
                SwingUtilities.updateComponentTreeUI(this);
                switched = true;
                break;
            }
            if (!switched) { /* ignore */ }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    // ------------------------
    // Welcome Panel
    // ------------------------
    private JPanel welcomePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(250, 250, 252));
        JLabel title = new JLabel("<html><div style='text-align:center'><b>CareConnect</b><br><small>Hospital Appointment System (Enhanced)</small></div></html>", SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.PLAIN, 26));
        title.setBorder(new EmptyBorder(25, 10, 10, 10));
        panel.add(title, BorderLayout.NORTH);

        JPanel center = new JPanel(new GridBagLayout());
        center.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 20, 10, 20);

        JButton btnLogin = new JButton("Login");
        JButton btnViewDoctors = new JButton("View Doctors");

        btnLogin.setPreferredSize(new Dimension(220, 40));
        btnViewDoctors.setPreferredSize(new Dimension(220, 40));

        gbc.gridx = 0; gbc.gridy = 0; center.add(btnLogin, gbc);
        gbc.gridy = 1; center.add(btnViewDoctors, gbc);

        panel.add(center, BorderLayout.CENTER);

        btnLogin.addActionListener(e -> cardLayout.show(mainPanel, "login"));
        btnViewDoctors.addActionListener(e -> showDoctorsDialog());

        JPanel footer = new JPanel();
        footer.setOpaque(false);
        footer.add(new JLabel("Made with ❤  •  Demo version (persistent to disk)"));
        panel.add(footer, BorderLayout.SOUTH);

        return panel;
    }

    // ------------------------
    // Login Panel
    // ------------------------
    private JPanel loginPanel() {
        loginPanelRef = new LoginPanel();
        return loginPanelRef;
    }

    private class LoginPanel extends JPanel {
        private JTextField txtUsername;
        private JPasswordField txtPassword;

        public LoginPanel() {
            setLayout(new GridBagLayout());
            setBorder(new EmptyBorder(30, 30, 30, 30));
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(8, 8, 8, 8);
            gbc.fill = GridBagConstraints.HORIZONTAL;

            JLabel lbl = new JLabel("Login to CareConnect");
            lbl.setFont(new Font("SansSerif", Font.BOLD, 18));
            gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
            add(lbl, gbc);
            gbc.gridwidth = 1;

            gbc.gridx = 0; gbc.gridy = 1; add(new JLabel("Username:"), gbc);
            txtUsername = new JTextField(20); gbc.gridx = 1; add(txtUsername, gbc);

            gbc.gridx = 0; gbc.gridy = 2; add(new JLabel("Password:"), gbc);
            txtPassword = new JPasswordField(20); gbc.gridx = 1; add(txtPassword, gbc);

            JButton btnSubmit = new JButton("Login");
            JButton btnBack = new JButton("Back");
            JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            btnPanel.add(btnBack); btnPanel.add(btnSubmit);
            gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2; add(btnPanel, gbc);

            btnBack.addActionListener(e -> cardLayout.show(mainPanel, "welcome"));
            btnSubmit.addActionListener(e -> doLogin());
        }

        private void doLogin() {
            String uname = txtUsername.getText().trim();
            String pass = new String(txtPassword.getPassword());

            Optional<User> opt = system.findUserByUsername(uname);
            if (!opt.isPresent()) {
                JOptionPane.showMessageDialog(this, "User not found.", "Login Failed", JOptionPane.ERROR_MESSAGE);
                return;
            }
            User u = opt.get();
            if (!u.verifyPassword(pass)) {
                JOptionPane.showMessageDialog(this, "Incorrect password.", "Login Failed", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (loginCallback != null) loginCallback.onLogin(u);
        }
    }

    // ------------------------
    // Patient Dashboard
    // ------------------------
    private JPanel patientDashboard() {
        patientPanelRef = new PatientPanel();
        return patientPanelRef;
    }

    private class PatientPanel extends JPanel {
        private CardLayout pCards = new CardLayout();
        private JPanel pCardPanel = new JPanel(pCards);
        private JComboBox<Doctor> doctorCombo;
        private JTextField txtDate;
        private JTextField txtTime;
        private JTextArea txtReason;
        private DefaultTableModel myApptModel;
        private JTextField searchField;
        private JComboBox<String> statusFilter;

        public PatientPanel() {
            setLayout(new BorderLayout());
            JLabel header = new JLabel("Patient Dashboard", SwingConstants.CENTER);
            header.setFont(new Font("SansSerif", Font.BOLD, 20));
            header.setBorder(new EmptyBorder(10, 10, 10, 10));
            add(header, BorderLayout.NORTH);

            JPanel left = new JPanel();
            left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
            left.setBorder(new EmptyBorder(10, 10, 10, 10));
            left.setPreferredSize(new Dimension(240, 0));
            JButton btnBook = new JButton("Book Appointment");
            JButton btnMyAppts = new JButton("My Appointments");
            JButton btnNotifications = new JButton("Notifications");

            btnBook.setAlignmentX(Component.CENTER_ALIGNMENT);
            btnMyAppts.setAlignmentX(Component.CENTER_ALIGNMENT);
            btnNotifications.setAlignmentX(Component.CENTER_ALIGNMENT);
            left.add(btnBook); left.add(Box.createRigidArea(new Dimension(0, 10)));
            left.add(btnMyAppts); left.add(Box.createRigidArea(new Dimension(0, 10)));
            left.add(btnNotifications);

            pCardPanel.add(buildBookingPanel(), "book");
            pCardPanel.add(buildMyAppointmentsPanel(), "list");
            pCardPanel.add(buildNotificationsPanel(), "notes");

            add(left, BorderLayout.WEST);
            add(pCardPanel, BorderLayout.CENTER);

            btnBook.addActionListener(e -> pCards.show(pCardPanel, "book"));
            btnMyAppts.addActionListener(e -> { reloadMyAppointments(); pCards.show(pCardPanel, "list"); });
            btnNotifications.addActionListener(e -> pCards.show(pCardPanel, "notes"));

            pCards.show(pCardPanel, "book");
        }

        private JPanel buildBookingPanel() {
            JPanel panel = new JPanel(new BorderLayout());
            JPanel form = new JPanel(new GridBagLayout());
            form.setBorder(new EmptyBorder(20, 20, 20, 20));
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(8, 8, 8, 8);
            gbc.fill = GridBagConstraints.HORIZONTAL;

            gbc.gridx = 0; gbc.gridy = 0; form.add(new JLabel("Choose Doctor:"), gbc);
            doctorCombo = new JComboBox<>();
            for (Doctor d : system.getDoctors()) doctorCombo.addItem(d);
            gbc.gridx = 1; form.add(doctorCombo, gbc);

            JButton viewSchedule = new JButton("View Schedule");
            gbc.gridx = 2; form.add(viewSchedule, gbc);
            viewSchedule.addActionListener(e -> showDoctorSlots((Doctor) doctorCombo.getSelectedItem()));

            gbc.gridx = 0; gbc.gridy = 1; form.add(new JLabel("Date (YYYY-MM-DD):"), gbc);
            txtDate = new JTextField(LocalDate.now().toString());
            gbc.gridx = 1; form.add(txtDate, gbc);

            gbc.gridx = 0; gbc.gridy = 2; form.add(new JLabel("Time (HH:mm):"), gbc);
            txtTime = new JTextField(LocalTime.now().plusHours(1).withMinute(0).toString());
            gbc.gridx = 1; form.add(txtTime, gbc);

            gbc.gridx = 0; gbc.gridy = 3; form.add(new JLabel("Reason / Notes:"), gbc);
            txtReason = new JTextArea(4, 20);
            JScrollPane reasonScroll = new JScrollPane(txtReason);
            gbc.gridx = 1; form.add(reasonScroll, gbc);

            JButton btnBook = new JButton("Confirm Booking");
            gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 3; form.add(btnBook, gbc);

            panel.add(form, BorderLayout.NORTH);

            btnBook.addActionListener(e -> {
                if (!(loggedInUser instanceof Patient)) { JOptionPane.showMessageDialog(this, "Not logged in as patient."); return; }
                Patient p = (Patient) loggedInUser;
                Doctor d = (Doctor) doctorCombo.getSelectedItem();
                String dateStr = txtDate.getText().trim();
                String timeStr = txtTime.getText().trim();
                String reason = txtReason.getText().trim();

                if (d == null || dateStr.isEmpty() || timeStr.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Please fill doctor, date, and time.", "Validation", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                try {
                    LocalDate date = LocalDate.parse(dateStr);
                    LocalTime time = LocalTime.parse(timeStr);
                    LocalDateTime dt = LocalDateTime.of(date, time);
                    Appointment appt = system.bookAppointment(p, d, dt, reason);
                    notifications.notify(String.format("New appointment #%d: %s with %s at %s", appt.getId(), p.getUsername(), d.getName(), dt.format(dtFormatter)));
                    JOptionPane.showMessageDialog(this, "Appointment booked: #" + appt.getId());
                    txtReason.setText("");
                    reloadMyAppointments();
                } catch (DateTimeParseException ex) {
                    JOptionPane.showMessageDialog(this, "Invalid date or time format.", "Error", JOptionPane.ERROR_MESSAGE);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "Failed to book: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            });

            return panel;
        }

        private void showDoctorSlots(Doctor d) {
            if (d == null) return;
            JDialog dialog = new JDialog(HospitalAppointmentAppEnhanced.this, "Available Slots - " + d.getName(), true);
            dialog.setSize(420, 380);
            dialog.setLocationRelativeTo(this);
            DefaultListModel<String> model = new DefaultListModel<>();
            for (LocalDateTime slot : d.getAvailableSlots()) {
                boolean taken = system.getAppointmentsForDoctor(d).stream().anyMatch(a -> a.getDateTime().equals(slot) && a.getStatus() == Appointment.Status.SCHEDULED);
                model.addElement(slot.format(dtFormatter) + (taken ? " (Taken)" : ""));
            }
            JList<String> list = new JList<>(model);
            list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            JScrollPane sp = new JScrollPane(list);
            JButton btnChoose = new JButton("Choose Slot");
            btnChoose.addActionListener(e -> {
                int sel = list.getSelectedIndex();
                if (sel >= 0) {
                    String value = model.getElementAt(sel);
                    if (value.contains("(Taken)")) { JOptionPane.showMessageDialog(dialog, "Slot not available."); return; }
                    String slotStr = value.substring(0, 16); // yyyy-MM-dd HH:mm
                    txtDate.setText(slotStr.substring(0,10));
                    txtTime.setText(slotStr.substring(11));
                    dialog.dispose();
                }
            });
            dialog.getContentPane().setLayout(new BorderLayout());
            dialog.getContentPane().add(sp, BorderLayout.CENTER);
            JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT)); bottom.add(btnChoose); dialog.getContentPane().add(bottom, BorderLayout.SOUTH);
            dialog.setVisible(true);
        }

        private JPanel buildMyAppointmentsPanel() {
            JPanel panel = new JPanel(new BorderLayout());
            panel.setBorder(new EmptyBorder(10,10,10,10));

            JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
            searchField = new JTextField(20); top.add(new JLabel("Search:")); top.add(searchField);
            statusFilter = new JComboBox<>(new String[]{"All","SCHEDULED","CANCELLED","COMPLETED"}); top.add(new JLabel("Status:")); top.add(statusFilter);
            JButton btnGo = new JButton("Filter"); top.add(btnGo);
            btnGo.addActionListener(e -> reloadMyAppointments());

            myApptModel = new DefaultTableModel(new Object[]{"ID","Doctor","When","Reason","Status","Action"}, 0) {
                @Override public boolean isCellEditable(int row, int column) { return column == 5; }
            };
            JTable table = new JTable(myApptModel);
            table.setRowHeight(28);

            table.getColumn("Action").setCellRenderer(new ButtonRenderer());
            table.getColumn("Action").setCellEditor(new ButtonEditor(new JCheckBox()) {
                @Override public void onClick(int row) {
                    int apptId = Integer.parseInt(myApptModel.getValueAt(row, 0).toString());
                    system.findAppointmentById(apptId).ifPresent(a -> {
                        if (a.getStatus() == Appointment.Status.SCHEDULED) {
                            int confirm = JOptionPane.showConfirmDialog(PatientPanel.this, "Cancel appointment #"+apptId+"?", "Confirm", JOptionPane.YES_NO_OPTION);
                            if (confirm == JOptionPane.YES_OPTION) {
                                a.cancel();
                                notifications.notify(String.format("Appointment #%d canceled by patient %s", a.getId(), a.getPatient().getUsername()));
                                reloadMyAppointments();
                            }
                        }
                    });
                }
            });

            panel.add(top, BorderLayout.NORTH);
            panel.add(new JScrollPane(table), BorderLayout.CENTER);
            return panel;
        }

        private JPanel buildNotificationsPanel() {
            JPanel panel = new JPanel(new BorderLayout());
            JTextArea area = new JTextArea(); area.setEditable(false);
            JButton btnRefresh = new JButton("Refresh");
            btnRefresh.addActionListener(e -> {
                area.setText("");
                for (String m : notifications.all()) area.append(m + "\n");
            });
            panel.add(new JScrollPane(area), BorderLayout.CENTER);
            panel.add(btnRefresh, BorderLayout.SOUTH);
            return panel;
        }

        private void reloadMyAppointments() {
            myApptModel.setRowCount(0);
            if (!(loggedInUser instanceof Patient)) return;
            Patient p = (Patient) loggedInUser;
            String q = searchField.getText().trim().toLowerCase();
            String status = (String) statusFilter.getSelectedItem();
            for (Appointment a : system.getAppointmentsForPatient(p)) {
                if (!"All".equals(status) && !a.getStatus().name().equals(status)) continue;
                if (!q.isEmpty()) {
                    boolean match = a.getDoctor().getName().toLowerCase().contains(q) || a.getReason().toLowerCase().contains(q) || String.valueOf(a.getId()).equals(q);
                    if (!match) continue;
                }
                myApptModel.addRow(new Object[]{ a.getId(), a.getDoctor().getName(), a.getDateTime().format(dtFormatter), a.getReason(), a.getStatus(), (a.getStatus() == Appointment.Status.SCHEDULED ? "Cancel" : "-") });
            }
        }

        public void refreshData() {
            doctorCombo.removeAllItems();
            for (Doctor d : system.getDoctors()) doctorCombo.addItem(d);
            reloadMyAppointments();
        }
    }

    // ------------------------
    // Admin Dashboard
    // ------------------------
    private JPanel adminDashboard() {
        adminPanelRef = new AdminPanel();
        return adminPanelRef;
    }

    private class AdminPanel extends JPanel {
        private DefaultTableModel doctorModel;
        private DefaultTableModel statsModel;

        public AdminPanel() {
            setLayout(new BorderLayout());
            JLabel header = new JLabel("Admin Dashboard", SwingConstants.CENTER);
            header.setFont(new Font("SansSerif", Font.BOLD, 20));
            header.setBorder(new EmptyBorder(10,10,10,10));
            add(header, BorderLayout.NORTH);

            JPanel center = new JPanel(new GridLayout(1,2));

            JPanel left = new JPanel(new BorderLayout()); left.setBorder(new EmptyBorder(10,10,10,10));
            doctorModel = new DefaultTableModel(new Object[]{"ID","Name","Spec","Email","Action"},0) { @Override public boolean isCellEditable(int r,int c){return c==4;} };
            JTable docTable = new JTable(doctorModel);
            docTable.setRowHeight(28);
            docTable.getColumn("Action").setCellRenderer(new ButtonRenderer());
            docTable.getColumn("Action").setCellEditor(new ButtonEditor(new JCheckBox()) { @Override public void onClick(int row){
                int docId = Integer.parseInt(doctorModel.getValueAt(row,0).toString());
                int confirm = JOptionPane.showConfirmDialog(AdminPanel.this, "Delete doctor #"+docId+"?","Confirm", JOptionPane.YES_NO_OPTION);
                if (confirm==JOptionPane.YES_OPTION) { system.removeDoctorByDocId(docId); refreshData(); }
            }});
            left.add(new JScrollPane(docTable), BorderLayout.CENTER);
            JButton btnAdd = new JButton("Add Doctor"); btnAdd.addActionListener(e -> showAddDoctorDialog()); left.add(btnAdd, BorderLayout.SOUTH);

            JPanel right = new JPanel(new BorderLayout()); right.setBorder(new EmptyBorder(10,10,10,10));
            statsModel = new DefaultTableModel(new Object[]{"Metric","Value"},0);
            JTable statsTable = new JTable(statsModel); statsTable.setRowHeight(26);
            right.add(new JScrollPane(statsTable), BorderLayout.NORTH);

            JTextArea upcoming = new JTextArea(); upcoming.setEditable(false);
            right.add(new JScrollPane(upcoming), BorderLayout.CENTER);

            center.add(left); center.add(right);
            add(center, BorderLayout.CENTER);

            refreshData();

            // update upcoming list every time refresh is called
            this.putClientProperty("upcomingArea", upcoming);
        }

        private void showAddDoctorDialog() {
            JTextField txtUser = new JTextField();
            JTextField txtPass = new JTextField();
            JTextField txtFn = new JTextField();
            JTextField txtLn = new JTextField();
            JTextField txtEmail = new JTextField();
            JTextField txtSpec = new JTextField();
            JPanel p = new JPanel(new GridLayout(0,1));
            p.add(new JLabel("Username:")); p.add(txtUser);
            p.add(new JLabel("Password:")); p.add(txtPass);
            p.add(new JLabel("First name:")); p.add(txtFn);
            p.add(new JLabel("Last name:")); p.add(txtLn);
            p.add(new JLabel("Email:")); p.add(txtEmail);
            p.add(new JLabel("Specialization:")); p.add(txtSpec);
            int res = JOptionPane.showConfirmDialog(this,p,"Add Doctor",JOptionPane.OK_CANCEL_OPTION);
            if (res==JOptionPane.OK_OPTION) {
                Doctor d = new Doctor(txtUser.getText(), txtPass.getText(), txtFn.getText(), txtLn.getText(), txtEmail.getText(), txtSpec.getText());
                system.addDoctor(d); refreshData();
            }
        }

        public void refreshData() {
            doctorModel.setRowCount(0);
            for (Doctor d : system.getDoctors()) doctorModel.addRow(new Object[]{d.getDocId(), d.getName(), d.getSpecialization(), d.getEmail(), "Delete"});

            statsModel.setRowCount(0);
            int total = system.getAppointments().size();
            long sched = system.getAppointments().stream().filter(a->a.getStatus()==Appointment.Status.SCHEDULED).count();
            long cancel = system.getAppointments().stream().filter(a->a.getStatus()==Appointment.Status.CANCELLED).count();
            long comp = system.getAppointments().stream().filter(a->a.getStatus()==Appointment.Status.COMPLETED).count();
            statsModel.addRow(new Object[]{"Total Appointments", total});
            statsModel.addRow(new Object[]{"Scheduled", sched});
            statsModel.addRow(new Object[]{"Cancelled", cancel});
            statsModel.addRow(new Object[]{"Completed", comp});

            JTextArea upcoming = (JTextArea) this.getClientProperty("upcomingArea");
            if (upcoming!=null) {
                upcoming.setText("");
                system.getAppointments().stream().filter(a->a.getStatus()==Appointment.Status.SCHEDULED).sorted(Comparator.comparing(Appointment::getDateTime)).limit(20).forEach(a-> upcoming.append(a.toString()+"\n"));
            }
        }
    }

    // ------------------------
    // Doctor Dashboard
    // ------------------------
    private JPanel doctorDashboard() {
        doctorPanelRef = new DoctorPanel();
        return doctorPanelRef;
    }

    private class DoctorPanel extends JPanel {
        private DefaultTableModel apptModel;

        public DoctorPanel() {
            setLayout(new BorderLayout());
            JLabel header = new JLabel("Doctor Dashboard", SwingConstants.CENTER);
            header.setFont(new Font("SansSerif", Font.BOLD, 20));
            header.setBorder(new EmptyBorder(10,10,10,10));
            add(header, BorderLayout.NORTH);

            apptModel = new DefaultTableModel(new Object[]{"ID","Patient","When","Reason","Status","Action"},0) { @Override public boolean isCellEditable(int r,int c){return c==5;} };
            JTable table = new JTable(apptModel); table.setRowHeight(28);
            table.getColumn("Action").setCellRenderer(new ButtonRenderer());
            table.getColumn("Action").setCellEditor(new ButtonEditor(new JCheckBox()) { @Override public void onClick(int row){
                int apptId = Integer.parseInt(apptModel.getValueAt(row,0).toString());
                system.findAppointmentById(apptId).ifPresent(a->{
                    if (a.getStatus()==Appointment.Status.SCHEDULED) {
                        int confirm = JOptionPane.showConfirmDialog(DoctorPanel.this, "Mark appointment #"+apptId+" as completed?","Confirm",JOptionPane.YES_NO_OPTION);
                        if (confirm==JOptionPane.YES_OPTION) { a.complete(); notifications.notify("Appointment #"+apptId+" marked completed by doctor."); refreshData(); }
                    }
                });
            }});

            add(new JScrollPane(table), BorderLayout.CENTER);
            JButton btnRefresh = new JButton("Refresh"); btnRefresh.addActionListener(e->refreshData()); add(btnRefresh, BorderLayout.SOUTH);
        }

        public void refreshData() {
            apptModel.setRowCount(0);
            if (!(loggedInUser instanceof Doctor)) return;
            Doctor d = (Doctor) loggedInUser;
            for (Appointment a : system.getAppointmentsForDoctor(d)) {
                apptModel.addRow(new Object[]{ a.getId(), a.getPatient().getUsername(), a.getDateTime().format(dtFormatter), a.getReason(), a.getStatus(), (a.getStatus()==Appointment.Status.SCHEDULED?"Complete":"-") });
            }
        }
    }

    // ------------------------
    // Reusable: Button Renderer / Editor
    // ------------------------
    private static class ButtonRenderer extends JButton implements javax.swing.table.TableCellRenderer {
        public ButtonRenderer() { setOpaque(true); }
        @Override public Component getTableCellRendererComponent(JTable table,Object value,boolean isSelected,boolean hasFocus,int row,int column) { setText(value==null?"":value.toString()); return this; }
    }

    private abstract class ButtonEditor extends DefaultCellEditor {
        protected JButton button;
        private boolean clicked;
        private int row;

        public ButtonEditor(JCheckBox checkBox) {
            super(checkBox);
            button = new JButton();
            button.setOpaque(true);
            button.addActionListener(e -> fireEditingStopped());
        }

        @Override public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) { button.setText((value==null)?"":value.toString()); this.row=row; clicked=true; return button; }
        @Override public Object getCellEditorValue() { if (clicked) onClick(row); clicked=false; return button.getText(); }
        @Override public boolean stopCellEditing() { clicked=false; return super.stopCellEditing(); }
        public abstract void onClick(int row);
    }

    // ------------------------
    // Doctor list dialog
    // ------------------------
    private void showDoctorsDialog() {
        StringBuilder sb = new StringBuilder();
        for (Doctor d : system.getDoctors()) sb.append(String.format("%d. %s (%s)\n", d.getDocId(), d.getName(), d.getSpecialization()));
        JOptionPane.showMessageDialog(this, sb.toString(), "Available Doctors", JOptionPane.INFORMATION_MESSAGE);
    }

    // ------------------------
    // Login callback
    // ------------------------
    private interface LoginCallback { void onLogin(User u); }
    private LoginCallback loginCallback;
    private void setLoginCallback(LoginCallback cb) { this.loginCallback = cb; }

    // ------------------------
    // Main
    // ------------------------
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            HospitalAppointmentAppEnhanced app = new HospitalAppointmentAppEnhanced();
            app.setVisible(true);
        });
    }
}
