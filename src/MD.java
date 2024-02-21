import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;

public class MD extends Canvas implements Runnable {
    int N = 1000; // number of molecules
    int canvasWidth = 400; // canvas width (and height) in pixels
    int pixelsPerUnit = 3; // number of pixels in one distance unit
    double boxWidth = canvasWidth * 1.0 / pixelsPerUnit;
    //We use double buffering to stop the flickering of the animation
    BufferedImage bf = new BufferedImage(canvasWidth, canvasWidth, BufferedImage.TYPE_INT_RGB);

    double[] x, y, vx, vy, ax, ay;
    double dt = 0.001;
    double halfDt = dt / 2;
    double halfDtSquare = dt * dt / 2;
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
        pictureFrame.add(canvasPanel);
        Panel controlPanel = new Panel();
        // add controls to controlPanel here
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

    private void computeAccelerations() {
        // Exert force from walls
        double wallStiffness = 50;
        for (int i = 0; i < N; i++) {
            if (x[i] < 0.5) {
                ax[i] = wallStiffness * (0.5 - x[i]);
            } else {
                if (x[i] > boxWidth - 0.5) {
                    ax[i] = wallStiffness * (boxWidth - 0.5 - x[i]);
                } else {
                    ax[i] = 0.0;
                }
            }
            if (y[i] < 0.5) {
                ay[i] = wallStiffness * (0.5 - y[i]);
            } else {
                if (y[i] > boxWidth - 0.5) {
                    ay[i] = wallStiffness * (boxWidth - 0.5 - y[i]);
                } else {
                    ay[i] = 0.0;
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
                if (rSquareRec > 0.111) {
                    double rToSixthRec = rSquareRec * rSquareRec * rSquareRec;
                    double rToEightRec = rToSixthRec * rSquareRec;
                    double rToFourteenthRec = rToEightRec * rToSixthRec;
                    double aPerR = 24 * (2 * rToFourteenthRec - rToEightRec);
                    ax[i] += aPerR * (x[i] - x[j]);
                    ay[i] += aPerR * (y[i] - y[j]);
                    ax[j] -= aPerR * (x[i] - x[j]);
                    ay[j] -= aPerR * (y[i] - y[j]);
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
    }
    public void run() {
        computeAccelerations();
        while(true) {
            for (int i = 0; i < 20; i++) {
                doStep();
            }
            //Update animation when the for loop done
            paint(this.getGraphics());
            //Make thread wait for drawing animation
            try { Thread.sleep(1); } catch (InterruptedException e) {}
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
