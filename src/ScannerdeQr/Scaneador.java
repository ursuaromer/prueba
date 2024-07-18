package ScannerdeQr;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamPanel;
import com.github.sarxos.webcam.WebcamResolution;
import com.google.zxing.*;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.awt.Dimension;
import java.util.Arrays;
import MenuInicio.MenuInicio;
import javax.sound.sampled.Clip;
import java.io.File;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;
import javax.mail.*;
import javax.mail.internet.*;
import java.util.Properties;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import java.io.FileOutputStream;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import java.io.IOException;
import java.net.URL;
import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import java.util.Timer;
import java.util.TimerTask;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

public class Scaneador extends javax.swing.JFrame {

    private Webcam webcam;
    private WebcamPanel webcamPanel;
    private ScheduledExecutorService executor;
    private String lastScannedQR = "";

    public Scaneador() {
        initComponents();
        setLocationRelativeTo(null);
        setTitle("SCANEADOR");
        playMP3("/Sonidos/piti.mp3");  // Nota la barra al principio
        initWebcam();
        startScan();

        //rsscalelabel.RSScaleLabel.setScaleLabel(jLabel2,"src/imagen/escanear.png");
    }

    private void playMP3(String mp3File) {
        try {
            System.out.println("Intentando reproducir: " + mp3File);
            URL resource = getClass().getResource(mp3File);
            if (resource == null) {
                System.out.println("No se pudo encontrar el archivo de sonido: " + mp3File);
                return;
            }
            System.out.println("Recurso encontrado: " + resource);

            String path = resource.toURI().toString();
            System.out.println("Path del archivo: " + path);
            Media media = new Media(path);
            MediaPlayer mediaPlayer = new MediaPlayer(media);

            mediaPlayer.setOnReady(() -> {
                System.out.println("Audio listo para reproducir");
                mediaPlayer.play();
            });

            mediaPlayer.setOnEndOfMedia(() -> {
                System.out.println("Reproducción finalizada");
                mediaPlayer.dispose();
            });

            mediaPlayer.setOnError(() -> {
                System.out.println("Error en la reproducción: " + mediaPlayer.getError());
            });

        } catch (Exception e) {
            System.out.println("Error al configurar la reproducción: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private String generatePDF(String[] data, String formattedDate, String formattedTime) {
        String fileName = "asistencia_" + System.currentTimeMillis() + ".pdf";
        try {
            Document document = new Document();
            PdfWriter.getInstance(document, new FileOutputStream(fileName));
            document.open();

            // Añadir el texto introductorio
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
            Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 12);

            Paragraph title = new Paragraph("Se ha registrado su asistencia:", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(20f);
            document.add(title);

            // Crear la tabla para los datos
            PdfPTable table = new PdfPTable(2);
            table.setWidthPercentage(100);
            table.setSpacingBefore(10f);
            table.setSpacingAfter(10f);

            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
            Font contentFont = FontFactory.getFont(FontFactory.HELVETICA, 12);

            String[] fields = {"Nombre", "Apellido", "DNI", "Carrera", "Ciclo", "Curso", "Fecha de registro", "Hora de registro"};

            for (int i = 0; i < fields.length; i++) {
                PdfPCell headerCell = new PdfPCell(new Phrase(fields[i], headerFont));
                headerCell.setBackgroundColor(BaseColor.LIGHT_GRAY);
                headerCell.setPadding(5);
                table.addCell(headerCell);

                PdfPCell valueCell;
                if (i < 3) {
                    valueCell = new PdfPCell(new Phrase(data[i], contentFont));
                } else if (i < 6) {
                    // Ajustamos los índices para saltar el email
                    valueCell = new PdfPCell(new Phrase(data[i + 1], contentFont));
                } else if (i == 6) {
                    valueCell = new PdfPCell(new Phrase(formattedDate, contentFont));
                } else {
                    valueCell = new PdfPCell(new Phrase(formattedTime, contentFont));
                }
                valueCell.setPadding(5);
                table.addCell(valueCell);
            }

            document.add(table);
            document.close();

            return fileName;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void sendEmail(String toEmail, String[] data) {
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");

        final String username = "qrsuizacorporationlasbqr@gmail.com";
        final String password = "w l k t e m b h d q h r z e t p";

        Session session = Session.getInstance(props, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(username));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            message.setSubject("Confirmación de Asistencia");

            // Obtener la fecha actual
            LocalDate currentDate = LocalDate.now();
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
            String formattedDate = currentDate.format(dateFormatter);

            // Obtener la hora actual
            LocalTime currentTime = LocalTime.now();
            DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
            String formattedTime = currentTime.format(timeFormatter);

            String emailContent = "Se ha registrado su asistencia. Por favor, revise el archivo PDF adjunto para más detalles.";

            // Generar el PDF
            String pdfFileName = generatePDF(data, formattedDate, formattedTime);

            // Crear el mensaje multipart
            Multipart multipart = new MimeMultipart();

            // Parte del texto del mensaje
            MimeBodyPart textPart = new MimeBodyPart();
            textPart.setText(emailContent);
            multipart.addBodyPart(textPart);

            // Parte del archivo adjunto
            if (pdfFileName != null) {
                MimeBodyPart pdfPart = new MimeBodyPart();
                FileDataSource source = new FileDataSource(pdfFileName);
                pdfPart.setDataHandler(new DataHandler(source));
                pdfPart.setFileName("Confirmacion_Asistencia.pdf");
                multipart.addBodyPart(pdfPart);
            }

            // Establecer el contenido del mensaje
            message.setContent(multipart);

            Transport.send(message);

            System.out.println("Correo enviado exitosamente a " + toEmail);

            // Borrar el archivo PDF temporal
            if (pdfFileName != null) {
                new File(pdfFileName).delete();
            }

        } catch (MessagingException e) {
            e.printStackTrace();
            System.out.println("Error al enviar el correo: " + e.getMessage());
        }
    }

    private void playSuccessSound() {
        try {
            // Parámetros del tono
            int sampleRate = 44100;  // 44100 muestras por segundo
            double durationSeconds = 0.1;  // Duración del tono en segundos
            double frequency = 1000;  // Frecuencia del tono en Hz

            // Generar el tono
            byte[] buf = new byte[(int) (sampleRate * durationSeconds)];
            for (int i = 0; i < buf.length; i++) {
                double angle = i / (sampleRate / frequency) * 2.0 * Math.PI;
                buf[i] = (byte) (Math.sin(angle) * 127.0 * 0.5);
            }

            // Configurar el formato de audio
            AudioFormat af = new AudioFormat(sampleRate, 8, 1, true, false);

            // Crear y reproducir el sonido
            SourceDataLine sdl = AudioSystem.getSourceDataLine(af);
            sdl.open(af);
            sdl.start();
            sdl.write(buf, 0, buf.length);
            sdl.drain();
            sdl.stop();
            sdl.close();
        } catch (Exception e) {
            System.out.println("Error al reproducir el sonido: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void initWebcam() {
        webcam = Webcam.getDefault();
        if (webcam == null) {
            System.out.println("No se detectó ninguna cámara.");
            return;
        }

        // Establecer la resolución de la cámara a HD (1280x720)
        webcam.setViewSize(WebcamResolution.QVGA.getSize());

        webcamPanel = new WebcamPanel(webcam);
        webcamPanel.setFPSDisplayed(true);
        webcamPanel.setDisplayDebugInfo(true);
        webcamPanel.setImageSizeDisplayed(true);
        webcamPanel.setMirrored(true);

        jPanel2.setLayout(new BorderLayout());
        jPanel2.add(webcamPanel, BorderLayout.CENTER);
        jPanel2.revalidate();
        jPanel2.repaint();

        System.out.println("Cámara iniciada con resolución: " + webcam.getViewSize().width + "x" + webcam.getViewSize().height);

        // Asegurarse de que la cámara esté abierta
        if (!webcam.isOpen()) {
            webcam.open();
        }
    }

    private void startScan() {
        if (webcam != null) {
            if (!webcam.isOpen()) {
                webcam.open();
            }

            if (webcam.isOpen()) {
                executor = Executors.newSingleThreadScheduledExecutor();
                executor.scheduleAtFixedRate(this::scanQR, 0, 100, TimeUnit.MILLISECONDS);
                System.out.println("Escaneo iniciado.");
            } else {
                System.out.println("No se pudo iniciar el escaneo. La cámara no está disponible.");
            }
        } else {
            System.out.println("No se pudo iniciar el escaneo. La cámara no está inicializada.");
        }
    }

    private void scanQR() {
        if (webcam.isOpen()) {
            try {
                BufferedImage image = webcam.getImage();
                if (image != null) {
                    LuminanceSource source = new BufferedImageLuminanceSource(image);
                    BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
                    Result result = new MultiFormatReader().decode(bitmap);

                    if (result != null) {
                        String scannedText = result.getText();
                        System.out.println("Código QR detectado: " + scannedText);

                        // Procesar el QR sin importar si es el mismo que el anterior
                        String[] lines = scannedText.split("\n");
                        if (lines.length >= 6) {
                            String[] data = new String[7];
                            for (int i = 0; i < 6; i++) {
                                String[] parts = lines[i].split(": ", 2);
                                if (parts.length == 2) {
                                    data[i] = parts[1];
                                }
                            }
                            // Procesar la carrera separadamente
                            if (lines.length > 5) {
                                String[] carreraParts = lines[4].split(":", 2);
                                if (carreraParts.length == 2) {
                                    data[4] = carreraParts[1].trim();
                                }
                                String[] cicloParts = lines[5].split(":", 2);
                                if (cicloParts.length == 2) {
                                    data[5] = cicloParts[1].trim();
                                }
                            }
                            data[6] = (String) jComboBox1.getSelectedItem();
                            System.out.println("Datos procesados: " + Arrays.toString(data));

                            SwingUtilities.invokeLater(() -> {
                                showData(data);
                                playSuccessSound();
                                sendEmail(data[3], data);
                                JOptionPane.showMessageDialog(this, "Asistencia guardada", "Éxito", JOptionPane.INFORMATION_MESSAGE);
                            });

                            // Pequeña pausa para evitar escaneos múltiples del mismo código
                            Thread.sleep(2000);
                        } else {
                            System.out.println("El código QR no contiene suficientes datos.");
                        }
                    }
                }
            } catch (NotFoundException ignored) {
                // No QR code found in this frame
            } catch (Exception e) {
                System.out.println("Error al escanear: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            System.out.println("La cámara no está abierta. Intentando abrir...");
            webcam.open();
        }
    }

    private void saveToDatabase(String[] data) {
        // Aquí irá la lógica para guardar en la base de datos
        // Por ahora, solo imprimiremos un mensaje
        System.out.println("Datos listos para ser guardados en la base de datos");
        // Ejemplo de cómo podrías estructurar los datos
        // String nombre = data[0];
        // String apellido = data[1];
        // String dni = data[2];
        // String email = data[3];
        // String carrera = data[4];
        // String ciclo = data[5];
        // String curso = jComboBox1.getSelectedItem().toString();
        // Date fechaHora = new Date(); // Fecha y hora actual

        // Aquí iría la lógica de conexión y guardado en la base de datos
    }

    private void showData(String[] data) {
        /* SwingUtilities.invokeLater(() -> {
            try {
                // ... (su código existente)

                // Resetear lastScannedQR después de 5 segundos
                Timer timer = new Timer();
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        //lastScannedQR = "";
                    }
                }, 5000); // 5000 milisegundos = 5 segundos

            } catch (Exception e) {
                System.out.println("Error al mostrar datos: " + e.getMessage());
                e.printStackTrace();
            }
        });*/
    }

    /* private void resetDataAfterDelay() {
        Timer timer = new Timer(5000, e -> {
            jTextField1.setText("");
            jTextField2.setText("");
            jTextField3.setText("");
            jTextField4.setText("");
            jTextField5.setText("");
            jTextField6.setText("");

            lastScannedQR = "";
        });
        timer.setRepeats(false);
        timer.start();
    }*/
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        jPanel1 = new javax.swing.JPanel();
        jPanel2 = new javax.swing.JPanel();
        jButton2 = new javax.swing.JButton();
        jLabel1 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jComboBox1 = new javax.swing.JComboBox<>();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        getContentPane().setLayout(new java.awt.CardLayout());

        jPanel1.setBackground(new java.awt.Color(0, 0, 0));
        jPanel1.setForeground(new java.awt.Color(0, 255, 255));
        jPanel1.setLayout(new java.awt.GridBagLayout());

        jPanel2.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(255, 255, 255)));
        jPanel2.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.gridheight = 3;
        gridBagConstraints.ipadx = 410;
        gridBagConstraints.ipady = 390;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(50, 190, 0, 0);
        jPanel1.add(jPanel2, gridBagConstraints);

        jButton2.setForeground(new java.awt.Color(255, 255, 255));
        jButton2.setIcon(new javax.swing.ImageIcon(getClass().getResource("/imagen/atras.png"))); // NOI18N
        jButton2.setText("RETROCESO");
        jButton2.setContentAreaFilled(false);
        jButton2.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        jButton2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton2ActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.ipadx = 43;
        gridBagConstraints.ipady = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(30, 50, 38, 0);
        jPanel1.add(jButton2, gridBagConstraints);

        jLabel1.setFont(new java.awt.Font("Verdana", 1, 24)); // NOI18N
        jLabel1.setForeground(new java.awt.Color(0, 255, 255));
        jLabel1.setText("SCANNER DE ASISTENCIA");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.ipadx = 13;
        gridBagConstraints.ipady = 20;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 210, 0, 0);
        jPanel1.add(jLabel1, gridBagConstraints);

        jLabel3.setFont(new java.awt.Font("Verdana", 1, 12)); // NOI18N
        jLabel3.setForeground(new java.awt.Color(255, 255, 255));
        jLabel3.setText("CURSO");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.ipadx = 22;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(50, 140, 0, 0);
        jPanel1.add(jLabel3, gridBagConstraints);

        jComboBox1.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "INGLES PARA LA COMUNICACION ORAL", "PROGRAMACION DISTRIBUIDA", "PROGRAMACION CONCURRENTE", "PROGRAMACION ORIENTADA A OBJETOS", "INVESTIGACION TECNOLOGICA", "EXPERIENCIA FORMATICAS SIT. REAL. TRAB.", "MODELAMIENTO DE SOFTWARE", "ARQUITECTURA DE BASE DE DATOS" }));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.ipadx = 105;
        gridBagConstraints.ipady = 14;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(24, 140, 0, 2);
        jPanel1.add(jComboBox1, gridBagConstraints);

        getContentPane().add(jPanel1, "card2");

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton2ActionPerformed
        MenuInicio menu = new MenuInicio();
        menu.setVisible(true);
        dispose();


    }//GEN-LAST:event_jButton2ActionPerformed
    @Override
    public void dispose() {
        if (executor != null) {
            executor.shutdown();
        }
        if (webcam != null) {
            webcam.close();
        }
        super.dispose();
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(Scaneador.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(Scaneador.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(Scaneador.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(Scaneador.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>
        com.sun.javafx.application.PlatformImpl.startup(() -> {
        });
        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new Scaneador().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButton2;
    private javax.swing.JComboBox<String> jComboBox1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    // End of variables declaration//GEN-END:variables
}
