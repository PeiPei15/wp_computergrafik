package computergraphics.datastructures.mesh;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import computergraphics.datastructures.bsp.BspTreeNode;
import computergraphics.datastructures.bsp.BspTreeToolsDummy;
import computergraphics.math.Vector;
import computergraphics.rendering.Texture;

public class Volume {

	// TriangleMeshes as Planes per Axis
	private TriangleMesh[][] triangleMeshes;
	// BSP-Tree for back-to-front-Sorting
	private BspTreeToolsDummy bspTool;
	private BspTreeNode[] rootNodes;
	// Centre Points of planes per Axis for back-to-front-sorting
	private List<Vector>[] centres;
	private List<Integer>[] centreIndices;
	private List<Integer>[] sortedCentreIndices;
	// needed in generateTexturesPerAxis(String axis) - better solution would be
	// appreciated
	private int texId = 0;
	// byteArray from raw-File
	private byte[] model;
	// Textures per Axis
	private Texture[][] textures;
	// color-byte-Arrays per Axis
	private byte[][][] colors;
	// Resolution per Axis
	private int[] resolution;

	private Vector eye;

	public Volume(String modelString, Vector eye) {

		this.eye = eye;
		/**
		 * bind texId without gl??? set interpolation set tex data texture
		 * planes?
		 */
		try {
			model = Files.readAllBytes(setModel(modelString));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		generateTexturePlanes();
	}

	/**
	 * enables this class to proceed back-to-front-sorting for texture-stack planes
	 * @param rootNode
	 * @param points
	 * @param eye
	 * @return
	 */
	public List<Integer> sortStackBackToFront(BspTreeNode rootNode, List<Vector> points, Vector eye) {
		return bspTool.getBackToFront(rootNode, points, eye);
	}

	private void generateTexturePlanes() {
		// stack Axis: X
		textures[0] = generateTexturesPerAxis(resolution[0]);
		colors[0] = generateColorByteArrays("x", resolution[1], resolution[2], resolution[0]);
		triangleMeshes[0] = (TriangleMesh[]) generateTriangleMeshPerAxis("x", resolution[0], centres[0], centreIndices[0]);
		rootNodes[0] = bspTool.createBspTree(null, centres[0], centreIndices[0]);
		// stack Axis: Y
		textures[1] = generateTexturesPerAxis(resolution[1]);
		colors[1] = generateColorByteArrays("y", resolution[0], resolution[2], resolution[1]);
		triangleMeshes[1] = (TriangleMesh[]) generateTriangleMeshPerAxis("y", resolution[1], centres[1], centreIndices[1]);
		rootNodes[1] = bspTool.createBspTree(null, centres[1], centreIndices[1]);
		// stack Axis: Z
		textures[2] = generateTexturesPerAxis(resolution[2]);
		colors[2] = generateColorByteArrays("z", resolution[0], resolution[1], resolution[2]);
		triangleMeshes[2] = (TriangleMesh[]) generateTriangleMeshPerAxis("z", resolution[2], centres[2], centreIndices[2]);
		rootNodes[2] = bspTool.createBspTree(null, centres[2], centreIndices[2]);
	}

	private Texture[] generateTexturesPerAxis(int res) {
		Texture[] t = new Texture[res];
		for (int i = 0; i <= res; i++) {
			t[i] = new Texture(texId);
			texId++;
		}
		return t;
	}

	private byte[][] generateColorByteArrays(String axis, int resA, int resB, int stackAxis) {

		int arySize = resA * resB * 4;
		byte[][] bAry = new byte[stackAxis][arySize];
		int accu = 0;

		// for each plane
		for (int i = 0; i <= stackAxis; i++) {
			for (int a = 0; a <= resA; a++) {
				for (int b = 0; b <= resB; b++) {
					// calculate color for each point within the plane
					byte[] colors = new byte[4];
					// ugly :(
					switch (axis) {
					case "x":
						colors = calcColor(i, a, b);
						break;
					case "y":
						colors = calcColor(a, i, b);
						break;
					case "z":
						colors = calcColor(a, b, i);
						break;
					}
					bAry[i][accu] = colors[0];
					bAry[i][accu + 1] = colors[1];
					bAry[i][accu + 2] = colors[2];
					bAry[i][accu + 3] = colors[3];
					accu += 4;
				}
			}
		}
		return bAry;
	}

	// see assignment 6 recommendation
	private byte[] calcColor(int x, int y, int z) {
		return new byte[] { getVolumeData(x, y, z), 0, 0, getVolumeData(x, y, z) };
	}

	/**
	 * this method may acces volume data
	 * 
	 * @param x
	 *            in 0...rX-1
	 * @param y
	 *            in 0...rY-1
	 * @param z
	 *            in 0...rZ-1
	 * @return volume data as byte
	 */
	public byte getVolumeData(int x, int y, int z) {
		// see assignment6
		return model[z * resolution[0] * resolution[1] + y + resolution[0] + x];
	}

	/**
	 * generates a simple triangle Mesh per plane, fills the list of
	 * Centre-Points and its indices
	 * 
	 * @param axis
	 * @param centrePoints
	 * @param centreIndices
	 * @return
	 */
	private ITriangleMesh[] generateTriangleMeshPerAxis(String axis, int res, List<Vector> centrePoints,
			List<Integer> centreIndices) {
		// draft see assets/notes/assignment6_1.JPG
		ITriangleMesh[] tAry = new TriangleMesh[res];
		for (int i = 0; i <= res; i++) {
			ITriangleMesh tM = new TriangleMesh();
			// see assignment 6 page 2
			int p = -1 + (2 * i) / (res - 1);
			Vector[] vectors = new Vector[4];
			switch (axis) {
			case "x":
				Vector[] vecX = { new Vector(p, 1, 1), new Vector(p, -1, 1), new Vector(p, 1, -1),
						new Vector(p, -1, -1) };
				vectors = vecX;
				break;
			case "y":
				Vector[] vecY = { new Vector(-1, p, 1), new Vector(1, p, 1), new Vector(-1, p, -1),
						new Vector(1, p, -1) };
				vectors = vecY;
				break;
			case "z":
				Vector[] vecZ = { new Vector(-1, -1, p), new Vector(1, -1, p), new Vector(-1, 1, p),
						new Vector(1, 1, p) };
				vectors = vecZ;
				break;
			}

			// Calculate Centre-Point of Plane and add to centrePoint-Array
			// a + ((d - a) * 0.5) --> connection-vector from a to d cut by a half
			Vector centre = vectors[0].add((vectors[3].subtract(vectors[0])).multiply(0.5));
			centrePoints.add(centre);
			centreIndices.add(i);

			// Add Triangles and Vertices to TriangleMesh
			// addVertices not tested!
			tM.addVertices(vectors); // a = 0; b = 1; c = 2; d = 3;

			Triangle t1 = new Triangle(2, 1, 0); // [c,b,a]
			Triangle t2 = new Triangle(2, 3, 1); // [c,d,b]

			tM.addTriangle(t1);
			tM.addTriangle(t2);
		}
		return tAry;
	}

	/**
	 * makes setup easier Resolution might need to be adjusted depending on
	 * hardware!
	 * 
	 * @param modelString
	 *            ["negship", "monkey", "engine", "foot"]
	 * @return path for given model
	 */
	private Path setModel(String modelString) {
		String s = "";
		switch (modelString) {
		case "negship":
			resolution[0] = 64;
			resolution[1] = 64;
			resolution[2] = 64;
			s = "assets/volumedata/neghip.vox";
			break;
		case "monkey":
			resolution[0] = 256;
			resolution[1] = 256;
			resolution[2] = 62;
			s = "assets/volumedata/monkey.raw";
			break;
		case "engine":
			resolution[0] = 256;
			resolution[1] = 256;
			resolution[2] = 256;
			s = "assets/volumedata/engine.raw";
			break;
		case "foot":
			resolution[0] = 256;
			resolution[1] = 256;
			resolution[2] = 256;
			s = "assets/volumedata/foot.raw";
			break;
		}
		return Paths.get(s);
	}

	
	public TriangleMesh[][] getTriangles() {
		return triangleMeshes;
	}

	public void setTriangles(TriangleMesh[][] triangleMeshes) {
		this.triangleMeshes = triangleMeshes;
	}

	public List<Integer>[] getSortedCentreIndices() {
		return sortedCentreIndices;
	}

	public void setSortedCentreIndices(List<Integer>[] sortedCentreIndices) {
		this.sortedCentreIndices = sortedCentreIndices;
	}

	public Texture[][] getTextures() {
		return textures;
	}

	public void setTextures(Texture[][] textures) {
		this.textures = textures;
	}

	public Vector getEye() {
		return eye;
	}

	public void setEye(Vector eye) {
		this.eye = eye;
	}

	/**
	 * To String to byteArray to check if it is correct
	 * 
	 * @param ary
	 * @return String of byte[]
	 */
	public String byteArrayToString(byte[] ary) {
		StringBuilder sb = new StringBuilder();
		int i = 0;
		for (byte b : ary) {
			sb.append("i = " + i).append(", b = " + b).append("\n");
			i++;
		}
		return sb.toString();
	}
}