package Animaciones;

import javax.swing.*;
import java.awt.*;

public class LoadingPanel extends JPanel {
    private float angle = 0;
    private Timer timer;

    public LoadingPanel() {
        setOpaque(false);
        timer = new Timer(15, e -> {
            angle += 0.15f;
            if (angle > 2 * Math.PI) {
                angle -= 2 * Math.PI;
            }
            repaint();
        });
        timer.start();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int centerX = getWidth() / 2;
        int centerY = getHeight() / 2;
        int radius = Math.min(getWidth(), getHeight()) / 15;

        for (int i = 0; i < 8; i++) {
            float pointAngle = angle - (i * 0.3f);
            int x = (int) (centerX + radius * Math.cos(pointAngle));
            int y = (int) (centerY + radius * Math.sin(pointAngle));

            int dotSize = 16 - (i * 2);
            g2d.setColor(new Color(30, 144, 255, 255 - (i * 30)));
            g2d.fillOval(x - dotSize/2, y - dotSize/2, dotSize, dotSize);
        }

        g2d.dispose();
    }
}