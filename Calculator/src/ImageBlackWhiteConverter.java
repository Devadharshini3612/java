import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

public class ImageBlackWhiteConverter extends JFrame {

    private BufferedImage originalImage;
    private JLabel imageLabel;

    public ImageBlackWhiteConverter() {
        setTitle("Image Black & White Converter");
        setSize(700, 600);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        // Image display area
        imageLabel = new JLabel("No Image Selected", JLabel.CENTER);
        imageLabel.setFont(new Font("Arial", Font.BOLD, 16));
        imageLabel.setOpaque(true);
        imageLabel.setBackground(Color.LIGHT_GRAY);
        imageLabel.setPreferredSize(new Dimension(600, 400));
        imageLabel.setBorder(BorderFactory.createLineBorder(Color.GRAY, 2));
        add(imageLabel, BorderLayout.CENTER);

        // Buttons panel
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 20, 10));

        JButton uploadBtn = new JButton("Upload Image");
        JButton convertBtn = new JButton("Convert to Black & White");

        uploadBtn.setFont(new Font("Arial", Font.BOLD, 14));
        convertBtn.setFont(new Font("Arial", Font.BOLD, 14));

        buttonPanel.add(uploadBtn);
        buttonPanel.add(convertBtn);
        add(buttonPanel, BorderLayout.SOUTH);

        // Action listeners
        uploadBtn.addActionListener(e -> chooseImage());
        convertBtn.addActionListener(e -> convertImage());

        setVisible(true);
    }

    private void chooseImage() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Select an Image");
        int result = fileChooser.showOpenDialog(this);

        if (result == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            try {
                originalImage = ImageIO.read(file);
                if (originalImage != null) {
                    displayImage(originalImage);
                } else {
                    JOptionPane.showMessageDialog(this, "Invalid image file.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error loading image: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void displayImage(BufferedImage img) {
        // Resize to fit panel while keeping aspect ratio
        int labelWidth = imageLabel.getWidth();
        int labelHeight = imageLabel.getHeight();
        int imgWidth = img.getWidth();
        int imgHeight = img.getHeight();

        double widthRatio = (double) labelWidth / imgWidth;
        double heightRatio = (double) labelHeight / imgHeight;
        double scale = Math.min(widthRatio, heightRatio);

        int newWidth = (int) (imgWidth * scale);
        int newHeight = (int) (imgHeight * scale);

        Image scaledImage = img.getScaledInstance(newWidth, newHeight, Image.SCALE_SMOOTH);
        imageLabel.setIcon(new ImageIcon(scaledImage));
        imageLabel.setText("");
    }

    private void convertImage() {
        if (originalImage == null) {
            JOptionPane.showMessageDialog(this, "Please upload an image first.", "No Image", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (isAlreadyBlackAndWhite(originalImage)) {
            JOptionPane.showMessageDialog(this, "The given image is already black and white.", "Info", JOptionPane.INFORMATION_MESSAGE);
        } else {
            BufferedImage bwImage = convertToBW(originalImage);
            originalImage = bwImage;
            displayImage(bwImage);
            JOptionPane.showMessageDialog(this, "Image has been converted to black and white.", "Done", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private boolean isAlreadyBlackAndWhite(BufferedImage img) {
        int width = img.getWidth();
        int height = img.getHeight();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = img.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;
                if (!(r == g && g == b)) {
                    return false;
                }
            }
        }
        return true;
    }

    private BufferedImage convertToBW(BufferedImage img) {
        int width = img.getWidth();
        int height = img.getHeight();
        BufferedImage bwImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = img.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;
                int gray = (r + g + b) / 3;
                int newRGB = (gray << 16) | (gray << 8) | gray;
                bwImage.setRGB(x, y, newRGB);
            }
        }
        return bwImage;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ImageBlackWhiteConverter::new);
    }
   
    

}
