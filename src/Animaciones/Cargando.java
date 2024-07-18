package Animaciones;

import javax.swing.*;
import java.awt.*;

public class Cargando extends JDialog {
    private LoadingPanel loadingPanel;
    private JFrame parentFrame;

    public Cargando(JFrame parent) {
        super(parent, "Cargando", true);
        this.parentFrame = parent;
        initComponents();
    }

    private void initComponents() {
        setUndecorated(true);
        setSize(parentFrame.getSize());
        setLocationRelativeTo(parentFrame);
        setBackground(new Color(0, 0, 0, 0));

        loadingPanel = new LoadingPanel();
        
        JPanel contentPanel = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                g.setColor(getBackground());
                g.fillRect(0, 0, getWidth(), getHeight());
                super.paintComponent(g);
            }
        };
        contentPanel.setOpaque(false);
        contentPanel.add(loadingPanel, BorderLayout.CENTER);
        
        setContentPane(contentPanel);
    }

    @Override
    public void setVisible(boolean visible) {
        if (visible) {
            setOpacity(0.7f);
        }
        super.setVisible(visible);
    }
}