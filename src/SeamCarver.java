import java.awt.Color;


//Array[height][width]
//Plane(width, height)
/************************************************
 *                width                         *
 *                                              *
 *                                              *
 * height                                       *
 *                                              *
 *                                              *
 *                                              *
 ************************************************/
public class SeamCarver {
  private int height, width;
  private int H, W;
  private int[][] point; //store the point message of current picture
  //private ArrayList<Integer> delete;
  private final Picture pic;
  
  public SeamCarver(Picture picture)
  {
      pic = new Picture(picture);
      height = picture.height();
      width = picture.width();
      H = height;
      W = width;
      point = new int[W][H];
     // delete = new ArrayList<Integer>();
      for (int x = 0; x < W; x++)
          for (int y = 0; y < H; y++)
              point[x][y] = convertPoint(x, y);
  }
  
  public Picture picture()                       // current picture
  {
      Picture recentPic = new Picture(width, height);
      //create picture
      for (int x = 0; x < width; x++)
          for (int y = 0; y < height; y++) {
              int xx = convertPoint(point[x][y])[0];
              int yy = convertPoint(point[x][y])[1];
              recentPic.set(x, y, pic.get(xx, yy)); 
          }
      return recentPic;
  }
  
  public int width()                         // width  of current picture
  { return this.width;  }
  
  public int height()                        // height of current picture
  { return this.height;  }
  
  public int[] findHorizontalSeam()            // sequence of indices for horizontal seam in current picture
  {
      double[][] energy = createEnergy();
      double[][] energyTo = createEnergyTo(energy);
      //printMatrix(energyTo);
      int[][] pixelTo = createPixelTo();
      int[] seam = new int[width];
      int id = 0;
      double min = Double.POSITIVE_INFINITY;
      //set energyTo & pixelTo
      for (int x = 0; x < width - 1; x++)
          for (int y = 0; y < height; y++) {
              relax(x, y, energy, energyTo, pixelTo);
              //printMatrix(pixelTo);
          }
      //get the bottom end point
      for (int y = 0; y < height; y++) {
          if (energyTo[width - 1][y] < min) {
              min = energyTo[width - 1][y];
              id = y;
          }
      }
      //get the path
      int front = pixelTo[width - 1][id]; //set end point
      seam[width - 1] = convertCurrentPoint(front)[1];
      for (int x = width - 2; x >= 0; x--) {
          int xx = convertCurrentPoint(front)[0];
          int yy = convertCurrentPoint(front)[1];
          seam[x] = yy;
          front = pixelTo[xx][yy];
      }
      
      return seam;
  }

  public int[] findVerticalSeam()              // sequence of indices for vertical   seam in current picture
  {
      reverseMatrix();
      int[] seam = findHorizontalSeam();
      reverseMatrix();
      return seam;
  }
  
  public void removeHorizontalSeam(int[] a)   // remove horizontal seam from current picture
  {
    //check input
      if (height <= 1 || a.length != width)
          throw new java.lang.IllegalArgumentException();
      for (int i = 0; i < a.length; i++) { 
          if (a[i] >= height || a[i] < 0) //cross border
              throw new java.lang.IllegalArgumentException();
          if (i != a.length - 1 && Math.abs(a[i + 1] - a[i]) > 1) // not a path
              throw new java.lang.IllegalArgumentException();
      }
      
      for (int x = 0; x < width; x++)
          for (int y = 0; y < height; y++) {
              if (y == a[x]) { // delete this pixel
                  //delete.add(point[x][y]);
                  System.arraycopy(point[x], y + 1, point[x], y, height - y - 1);
              }  
          }
      height--;
  }
  
  public void removeVerticalSeam(int[] a)     // remove vertical   seam from current picture
  {
      reverseMatrix();
      removeHorizontalSeam(a);
      reverseMatrix();
  }
  
  private double deltaX2(int x, int y)
  {
      //(x, y) is the point of the origin picture 
      //(xx, yy) is the position of origin picture
      int xx;
      int yy;
      xx = convertPoint(point[x - 1][y])[0];
      yy = convertPoint(point[x - 1][y])[1];
      Color pl = pic.get(xx, yy);
      xx = convertPoint(point[x + 1][y])[0];
      yy = convertPoint(point[x + 1][y])[1];
      Color pr = pic.get(xx, yy);
      
      double dred     = pl.getRed() - pr.getRed();
      double dgreen   = pl.getGreen() - pr.getGreen();
      double dblue    = pl.getBlue() - pr.getBlue();
      return dred * dred + dgreen * dgreen + dblue * dblue;
  }

  private double deltaY2(int x, int y)
  {
      //(x, y) is the point of the origin picture   
      //(xx, yy) is the position of origin picture
      int xx;
      int yy;
      xx = convertPoint(point[x][y - 1])[0];
      yy = convertPoint(point[x][y - 1])[1];
      Color pu = pic.get(xx, yy);
      xx = convertPoint(point[x][y + 1])[0];
      yy = convertPoint(point[x][y + 1])[1];
      Color pd = pic.get(xx, yy);
      
      double dred     = pu.getRed() - pd.getRed();
      double dgreen   = pu.getGreen() - pd.getGreen();
      double dblue    = pu.getBlue() - pd.getBlue();
      return dred * dred + dgreen * dgreen + dblue * dblue;
  }
  
 /* private boolean isDelete(int n)
  {
      return delete.contains(n);
  }*/
  
  public double energy(int x, int y)            // energy of pixel at column x and row y in current picture
  {   
      //(x, y) is the position of current picture
      if (x < 0 || y < 0 || x > width - 1 || y > height - 1)
          throw new java.lang.IndexOutOfBoundsException();
      if (x == 0 || y == 0 || x == width - 1 || y == height - 1)
          return 3 * 255 * 255.0; //195075.0
      return deltaX2(x, y) + deltaY2(x, y);
  }
  
  private double[][] createEnergy()
  { 
      double[][] energy = new double[width][height];
      for (int x = 0; x < width; x++) {
          for (int y = 0; y < height; y++) {
              //(x, y) is the position in the energy array
              energy[x][y] = energy(x, y);
          }
      }
      //printEnergy(energy);
      return energy;
  }
  
 /* private void printMatrix(double[][] energy)
  {
      StdOut.println("\t\t------Start-------");
      for (int y = 0; y < height; y++) 
          for (int x = 0; x < width; x++) {
              StdOut.printf(" %6.0f  ", energy[x][y]);
              if (x == width - 1) 
                  StdOut.println();
          }
      StdOut.println("\t\t-------End--------");
  }
  private void printMatrix(int[][] energy)
  {
      StdOut.println("\t\t------Start-------");
      for (int y = 0; y < height; y++) 
          for (int x = 0; x < width; x++) {
              StdOut.printf(" (%2d, %2d)  ", convertPoint(energy[x][y])[0],convertPoint(energy[x][y])[1]);
              if (x == width - 1) 
                  StdOut.println();
          }
      StdOut.println("\t\t-------End--------");
  }
  */
  private double[][] createEnergyTo(double[][] energy)
  {
      double[][] energyTo = new double[width][height];
      for (int x = 0; x < width; x++)
          for (int y = 0; y < height; y++) {
              energyTo[x][y] = Double.POSITIVE_INFINITY;
              if (x == 0) // start point
                  energyTo[x][y] = energy[x][y]; //border case
          } 
      return energyTo;
  }
  
  private int[][] createPixelTo()
  {
      int[][] pixelTo = new int[width][height];
      return pixelTo;
  }

  private int   convertPoint(int x, int y)
  {
      return y * pic.width() + x;
  }

  private int[] convertPoint(int n)
  {
      return new int[] {n % pic.width(), n / pic.width()};
  }
  
  private int convertCurrentPoint(int x, int y)
  {
      return y * width + x;
  }
  
  private int[] convertCurrentPoint(int n)
  {
      return new int[] {n % width, n / width};
  }
  //relax edge e
  private void relax(int x, int y, double[][] energy, double[][] energyTo, int[][] pixelTo) {
      //(x, y) is the position of the current picture
      if (y != 0 && y != height -1) {  //non-border case
          for (int d = -1; d < 2; d++) {
              if (energyTo[x + 1][y + d] > energyTo[x][y] + energy[x + 1][y + d]) {
                  energyTo[x + 1][y + d] = energyTo[x][y] + energy[x + 1][y + d];
                  pixelTo[x + 1][y + d] = convertCurrentPoint(x, y);
                  //StdOut.printf("NB-Set:pT[%2d][%2d] = %6d\n",x+1,y+d,convertPoint(x, y));
              }
          }
      }
      else if (y == 0) { //border case top left
          for (int d = 0; d < 2; d++) {
              if (energyTo[x + 1][y + d] > energyTo[x][y] + energy[x + 1][y + d]) {
                  energyTo[x + 1][y + d] = energyTo[x][y] + energy[x + 1][y + d];
                  pixelTo[x + 1][y + d] = convertCurrentPoint(x, y);
                  //StdOut.printf("TL-Set:pT[%2d][%2d] = %6d\n",x+1,y+d,convertPoint(x, y));
              }
          }
      }
      else if (y == height - 1) { //border case bottom left
          for (int d = -1; d < 1; d++) {
              if (energyTo[x + 1][y + d] > energyTo[x][y] + energy[x + 1][y + d]) {
                  energyTo[x + 1][y + d] = energyTo[x][y] + energy[x + 1][y + d];
                  pixelTo[x + 1][y + d] = convertCurrentPoint(x, y);
                  //StdOut.printf("BL-Set:pT[%2d][%2d] = %6d\n",x+1,y+d,convertPoint(x, y));
              }
          }
      }
  }

  //reverse the color[][]
  private void reverseMatrix()
  {
      //swap H <-> W
      int temp = width;
      width = height;
      height = temp;
      temp = H;
      H = W;
      W = temp;
      //transpose color[][]
      int[][] colort = point; //colort - height x width
      point = new int[width][height];
      for (int x = 0; x < width; x++)
          for (int y = 0; y < height; y++) {
              point[x][y] = colort[y][x];
          }
  }
  
  public static void main(String[] args) 
  {
  }

}
