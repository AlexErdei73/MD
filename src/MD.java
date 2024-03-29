import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.text.DecimalFormat;

public class MD extends Canvas implements Runnable {
    int N = 1000; // number of molecules
    int canvasWidth = 600; // canvas width (and height) in pixels
    int pixelsPerUnit = 3; // number of pixels in one distance unit
    double boxWidth = canvasWidth * 1.0 / pixelsPerUnit;
    //We use double buffering to stop the flickering of the animation
    BufferedImage bf = new BufferedImage(canvasWidth, canvasWidth, BufferedImage.TYPE_INT_RGB);

    double[] x, y, vx, vy, ax, ay;
    double dt = 0.001;
    double halfDt = dt / 2;
    double halfDtSquare = dt * dt / 2;
    boolean running = false;
    double t, kinEnergy, potEnergy, sumOfEnergy, measurementNumber, sumOfForce, externalForce;
    double cutOffDistanceSquare = 0.111;
    double potEnergyCorrection = 4 * (Math.pow(cutOffDistanceSquare, 6) - Math.pow(cutOffDistanceSquare, 3));
    Canvas dataCanvas;
    DecimalFormat fourDigit = new DecimalFormat("0.0000");
    MD() {
        setSize(canvasWidth, canvasWidth);
        Frame pictureFrame = new Frame("Molecular Dynamics");
        pictureFrame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                super.windowClosing(e);
                System.exit(0);
            }
        });
        Panel canvasPanel = new Panel();
        canvasPanel.add(this);
        pictureFrame.add(canvasPanel, BorderLayout.NORTH);
        Panel dataPanel = new Panel();
        dataPanel.setLayout(new GridLayout(0, 1));
        dataCanvas = new Canvas() {
            public void paint(Graphics g) {
                g.drawString("t = " + fourDigit.format(t), 5, 15);
                g.drawString("kinetic E = " + fourDigit.format(kinEnergy), 5, 30);
                g.drawString("pot Energy = " + fourDigit.format(potEnergy), 5, 45);
                g.drawString("energy = " + fourDigit.format(potEnergy + kinEnergy), 5, 60);
                double temp = sumOfEnergy / N / measurementNumber;
                g.drawString("temp = " + fourDigit.format(temp), 5, 75);
                double pressure = sumOfForce / 4 / boxWidth / measurementNumber;
                g.drawString("pressure = " + fourDigit.format(pressure), 5, 90);
            }
        };
        dataCanvas.setSize(canvasWidth, 105);
        dataPanel.add(dataCanvas);
        Panel controlPanel = new Panel();
        controlPanel.setLayout(new GridLayout(0, 3));
        Button startBtn = new Button("Start");
        startBtn.addActionListener(new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent actionEvent) {
            running = !running;
            if (running) {
              startBtn.setLabel("Stop");
            } else {
              startBtn.setLabel("Start");
            }
          }
        });
        Button energyUpBtn = new Button("E Up");
        Button energyDownBtn = new Button("E Down");
        energyUpBtn.addActionListener(new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent actionEvent) {
            increaseEnergy();
            resetData();
          }
        });
        energyDownBtn.addActionListener(new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent actionEvent) {
            decreaseEnergy();
            resetData();
          }
        });
        Button resetDataBtn = new Button("Reset");
        resetDataBtn.addActionListener(new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent actionEvent) {
            resetData();
          }
        });
        Button logDataBtn = new Button("Log Data");
        logDataBtn.addActionListener(new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent actionEvent) {
            System.out.println("T=" + fourDigit.format(sumOfEnergy / measurementNumber / N) + "  " +
              "P=" + fourDigit.format(sumOfForce / measurementNumber / 4 / boxWidth));
          }
        });
        controlPanel.add(startBtn);
        controlPanel.add(energyUpBtn);
        controlPanel.add(energyDownBtn);
        controlPanel.add(resetDataBtn);
        controlPanel.add(logDataBtn);
        pictureFrame.add(dataPanel);
        pictureFrame.add(controlPanel, BorderLayout.SOUTH);
        pictureFrame.pack();
        pictureFrame.setVisible(true);

        //Initialise the arrays
        x = new double[N];
        y = new double[N];
        vx = new double[N];
        vy = new double[N];
        ax = new double[N];
        ay = new double[N];

        //Set some positions
        double sqrN = Math.floor(Math.sqrt(N)) + 1;
        double width = boxWidth / (sqrN + 1);
        double offset;
        if (width > 1.5) width = 1.5;
        offset = (boxWidth - sqrN * width) / 2;
        int index;
        for (int col = 0; col < sqrN; col++) {
            for (int row = 0; row < sqrN; row++) {
                index = (int) (row *  sqrN) + col;
                if (index < N) {
                    x[index] = offset + col * width + 0.5;
                    y[index] = offset + row * width + 0.5;
                }
            }
        }

        //Set initial velocities
        vx[0] = 100;

        t = 0;
        resetData();

        Thread simulationThread = new Thread(this);
        simulationThread.start();   //it executes the run method
    }

    private void updatePositions() {
        for (int i = 0; i < N; i++) {
            x[i] += vx[i] * dt + ax[i] * halfDtSquare;
            y[i] += vy[i] * dt + ay[i] * halfDtSquare;
        }
    }

    private void updateVelocities() {
        for (int i = 0; i < N; i++) {
            vx[i] += ax[i] * halfDt;
            vy[i] += ay[i] * halfDt;
        }
    }

    private void increaseEnergy() {
      for (int i=0; i < N; i++) {
        vx[i] = 1.1 * vx[i];
        vy[i] = 1.1 * vy[i];
      }
    }

    private void decreaseEnergy() {
      for (int i=0; i < N; i++) {
        vx[i] = vx[i] / 1.1;
        vy[i] = vy[i] / 1.1;
      }
    }

    private void computeData() {
      kinEnergy = 0;
      for (int i=0; i < N; i++) {
        kinEnergy += 0.5 * (vx[i]*vx[i] + vy[i]*vy[i]);
      }
      sumOfEnergy += kinEnergy;
      sumOfForce += externalForce;
      measurementNumber++;
    }

    private void resetData() {
      sumOfEnergy = 0;
      sumOfForce = 0;
      measurementNumber = 0;
    }

    private void computeAccelerations() {
        // Exert force from walls
        double wallStiffness = 50;
        potEnergy = 0;
        externalForce = 0;
        for (int i = 0; i < N; i++) {
            if (x[i] < 0.5) {
                ax[i] = wallStiffness * (0.5 - x[i]);
                externalForce += ax[i];
                potEnergy += 0.5 * wallStiffness * (0.5 - x[i]) * (0.5 - x[i]);
            } else {
                if (x[i] > boxWidth - 0.5) {
                    ax[i] = wallStiffness * (boxWidth - 0.5 - x[i]);
                    externalForce -= ax[i];
                    potEnergy += 0.5 * wallStiffness * (boxWidth - 0.5 - x[i]) * (boxWidth - 0.5 - x[i]);
                } else {
                    ax[i] = 0.0;
                }
            }
            if (y[i] < 0.5) {
                ay[i] = wallStiffness * (0.5 - y[i]);
                externalForce += ay[i];
                potEnergy += 0.5 * wallStiffness * (0.5 - y[i]) * (0.5 - y[i]);
            } else {
                if (y[i] > boxWidth - 0.5) {
                    ay[i] = wallStiffness * (boxWidth - 0.5 - y[i]);
                    externalForce -= ay[i];
                    potEnergy += 0.5 * wallStiffness * (boxWidth - 0.5 - y[i]) * (boxWidth - 0.5 - y[i]);
                } else {
                    ay[i]= 0.0;
                }
            }
        }

        //Exert collision forces
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < i; j++) {
                // compute forces between molecules i and j
                // and add on to the accelerations of both
                double rSquareRec = 1.0 / ((x[i] - x[j]) * (x[i] - x[j]) + (y[i] - y[j]) * (y[i] - y[j]));
                // Take into account interaction only for close particles
                if (rSquareRec > cutOffDistanceSquare) {
                    double rToSixthRec = rSquareRec * rSquareRec * rSquareRec;
                    double rToTwelveRec = rToSixthRec * rToSixthRec;
                    double rToEightRec = rToSixthRec * rSquareRec;
                    double rToFourteenthRec = rToEightRec * rToSixthRec;
                    double aPerR = 24 * (2 * rToFourteenthRec - rToEightRec);
                    ax[i] += aPerR * (x[i] - x[j]);
                    ay[i] += aPerR * (y[i] - y[j]);
                    ax[j] -= aPerR * (x[i] - x[j]);
                    ay[j] -= aPerR * (y[i] - y[j]);
                    potEnergy += 4 * (rToTwelveRec - rToSixthRec) - potEnergyCorrection;
                }
             }
        }
    }

    private void doStep() {
        //Do simulation step here
        updatePositions();
        updateVelocities();
        computeAccelerations();
        updateVelocities();
        t += dt;
    }
    public void run() {
        computeAccelerations();
        while(true) {
          if (running) {
            for (int i = 0; i < 20; i++) {
              doStep();
            }
            //Update animation when the for loop done
            paint(this.getGraphics());
            computeData();
            dataCanvas.repaint();
            //Make thread wait for drawing animation
            try { Thread.sleep(5); } catch (InterruptedException e) {}
          }
        }
    }

    public void paint(Graphics g) {
        //Draw here
        //We are drawing on the buffer instead of the canvas
        Graphics bg = bf.getGraphics();
        bg.setColor(Color.white);
        bg.fillRect(0, 0, this.canvasWidth, this.canvasWidth);    //background rectangle
        bg.setColor(Color.blue);
        int x, y, r;
        for (int i = 0; i < N; i++) {
          x = (int) Math.round(this.x[i] * pixelsPerUnit);
          y = (int) Math.round(this.y[i] * pixelsPerUnit);
          r = pixelsPerUnit / 2;
          bg.drawOval(x - r, y - r, pixelsPerUnit, pixelsPerUnit);
        }
        //After drawing we flush the content of the buffer to the canvas
        //This is atomic operation and no screen refreshment is happening during this
        //Hence no more flickering of the animation
        g.drawImage(bf, 0, 0, null);
    }
    public static void main(String[] args) {
        new MD();
    }
}
