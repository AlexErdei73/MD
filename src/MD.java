import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;

public class MD extends Canvas implements Runnable {
    int N = 50; // number of molecules
    int canvasWidth = 400; // canvas width (and height) in pixels
    int pixelsPerUnit = 20; // number of pixels in one distance unit
    double boxWidth = canvasWidth * 1.0 / pixelsPerUnit;
    //We use double buffering to stop the flickering of the animation
    BufferedImage bf = new BufferedImage(canvasWidth, canvasWidth, BufferedImage.TYPE_INT_RGB);
    int counter = 0;

    double[] x, y, vx, vy, ax, ay;
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

        Thread simulationThread = new Thread(this);
        simulationThread.start();   //it executes the run method
    }

    private void doStep() {
        //Do simulation step here
        this.counter++;
    }
    public void run() {
        while(true) {
            for (int i = 0; i < 10; i++) {
                doStep();
            }
            //Update animation when the for loop done
            paint(this.getGraphics());
            //Make thread wait for drawing animation
            try { Thread.sleep(30); } catch (InterruptedException e) {}
        }
    }

    public void paint(Graphics g) {
        //Draw here
        //We are drawing on the buffer instead of the canvas
        Graphics bg = bf.getGraphics();
        bg.setColor(Color.white);
        bg.fillRect(0, 0, this.canvasWidth, this.canvasWidth);    //background rectangle
        bg.setColor(Color.blue);
        bg.drawString("Counter: " + this.counter, 0, this.canvasWidth);
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
