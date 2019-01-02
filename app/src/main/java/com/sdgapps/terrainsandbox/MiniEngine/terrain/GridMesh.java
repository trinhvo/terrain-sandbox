package com.sdgapps.terrainsandbox.MiniEngine.terrain;

import android.opengl.GLES30;

import com.sdgapps.terrainsandbox.MiniEngine.graphics.glsl.GLSLProgram;
import com.sdgapps.terrainsandbox.Singleton;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

//TODO might be worth it to use short for indices and save some memory
public class GridMesh {

    public static final int FloatBytes = Float.SIZE / 8;
    public static final int IntBytes = Integer.SIZE / 8;
    public static final int ShortBytes = Short.SIZE / 8;

    //grid size in vertices
    private int vdim = 0;
    int gridDim = 0;
    private int[] index_array;
    private float[] gridPositions_array;

    private int buffers[]; //contains buffer id of the whole chunk

    private short[] baryCoordsArray;
    private int[] offsets = new int[4];

    private int indexArraySize;
    private int partialArraySize;
    boolean uploadedVBO = false;

    GridMesh(int vertexWidth) {
        vdim = vertexWidth;
        gridDim = vdim - 1;
        GenVertexArray();
        GenIndexArray();
        GenBarycentricCoords();
        GenBuffersAndSubmitToGL();
    }

    private void GenVertexArray() {
        int nverts = vdim * vdim;
        gridPositions_array = new float[nverts * 2];  //xy

        for (int i = 0; i < nverts * 2; i += 2) {
            int nvert = i / 2;
            int X = nvert % vdim;
            int Z = nvert / vdim;

            gridPositions_array[i] = X;
            gridPositions_array[i + 1] = Z;
        }
    }

    /**
     * Index arrangement:
     * <p>
     * Divide the mesh in 4 square areas
     * <p>
     * The index array contains the triangles of each area in the following order:
     * <p>
     * [bottomLeft|bottomRight|topLeft|topRight]
     * <p>
     * This arrangement allows for rendering of the whole mesh in one go
     * or each quarter separately adjusting offset and size values (in the glDrawElements call) without using
     * extra memory on separate index arrays for each quarter.
     */
    private void GenIndexArray() {

        indexArraySize = 6 * (gridDim * gridDim);
        index_array = new int[indexArraySize];
        int start;
        int k = 0;

        int halfd = vdim / 2;
        int fulld = gridDim;

        int sq=0;
        partialArraySize = indexArraySize / 4;
        for (int i = 0; i < halfd; i++) { //row
            for (int j = 0; j < halfd; j++) { //column

                start = (((fulld + 1) * i) + j);
                /*
                 * triangle 1
                 *                v2
                 *                  *-------o
                 *                  |       |
                 *                  |       |
                 *             v1   *-------* v3
                 */

                index_array[k++] = start; //v1
                index_array[k++] = start + (fulld + 1);//v2
                index_array[k++] = start + 1;//v3


                /*
                 * triangle 2
                 *                  v4       v5
                 *                  *-------*
                 *                  |       |
                 *                  |       |
                 *                  o-------* v6
                 */

                index_array[k++] = start + (fulld + 1); //v4
                index_array[k++] = start + (fulld + 2); //v5
                index_array[k++] = start + 1; //v6

            }
        }

        offsets[0] = 0;
        offsets[1] = k * IntBytes;

        for (int i = 0; i < halfd; i++) { //row
            for (int j = halfd; j < fulld; j++) { //column

                start = (((fulld + 1) * i) + j);

                index_array[k++] = start; //v1
                index_array[k++] = start + (fulld + 1);//v2
                index_array[k++] = start + 1;//v3

                index_array[k++] = start + (fulld + 1); //v4
                index_array[k++] = start + (fulld + 2); //v5
                index_array[k++] = start + 1; //v6
            }
        }
        offsets[2] = k * IntBytes;
        for (int i = halfd; i < fulld; i++) { //row
            for (int j = 0; j < halfd; j++) { //column

                start = (((fulld + 1) * i) + j);

                index_array[k++] = start; //v1
                index_array[k++] = start + (fulld + 1);//v2
                index_array[k++] = start + 1;//v3

                index_array[k++] = start + (fulld + 1); //v4
                index_array[k++] = start + (fulld + 2); //v5
                index_array[k++] = start + 1; //v6
            }
        }

        /*Offset in glDrawElements is measured in bytes*/
        offsets[3] = k * IntBytes;
        for (int i = halfd; i < fulld; i++) { //row
            for (int j = halfd; j < fulld; j++) { //column

                start = (((fulld + 1) * i) + j);

                index_array[k++] = start; //v1
                index_array[k++] = start + (fulld + 1);//v2
                index_array[k++] = start + 1;//v3

                index_array[k++] = start + (fulld + 1); //v4
                index_array[k++] = start + (fulld + 2); //v5
                index_array[k++] = start + 1; //v6
            }
        }
    }

    private void GenBarycentricCoords() {
        short nextvalue = 0;
        short rowStartValue = 0;

        baryCoordsArray = new short[vdim * vdim * 3];

        for (int j = 0; j < vdim; j++) {
            for (int i = 0; i < vdim; i++) {
                if (nextvalue == 0)//0 1 0
                {
                    baryCoordsArray[3 * i + 3 * vdim * j] = 0;
                    baryCoordsArray[3 * i + 1 + 3 * vdim * j] = 1;
                    baryCoordsArray[3 * i + 2 + 3 * vdim * j] = 0;
                } else if (nextvalue == 1) //1,0 0
                {
                    baryCoordsArray[3 * i + 3 * vdim * j] = 1;
                    baryCoordsArray[3 * i + 1 + 3 * vdim * j] = 0;
                    baryCoordsArray[3 * i + 2 + 3 * vdim * j] = 0;
                } else //0,0,1
                {
                    baryCoordsArray[3 * i + 3 * vdim * j] = 0;
                    baryCoordsArray[3 * i + 1 + 3 * vdim * j] = 0;
                    baryCoordsArray[3 * i + 2 + 3 * vdim * j] = 1;
                }

                nextvalue = (short) ((nextvalue + 1) % 3);
            }
            rowStartValue = (short) ((rowStartValue + 2) % 3);
            nextvalue = rowStartValue;
        }
    }

    public void GenBuffersAndSubmitToGL() {

        if (!uploadedVBO) {
            //gen buffers
            ByteBuffer fBufW = ByteBuffer.allocateDirect(index_array.length * IntBytes);
            fBufW.order(ByteOrder.nativeOrder());
            IntBuffer indexBuffer = fBufW.asIntBuffer();
            indexBuffer.put(index_array);
            indexBuffer.position(0);


            ByteBuffer aBuf = ByteBuffer.allocateDirect(gridPositions_array.length * FloatBytes);
            aBuf.order(ByteOrder.nativeOrder());
            FloatBuffer gridPositionsBuffer = aBuf.asFloatBuffer();
            gridPositionsBuffer.put(gridPositions_array);
            gridPositionsBuffer.position(0);
            //gridPositions_array = null;

            aBuf = ByteBuffer.allocateDirect(baryCoordsArray.length * ShortBytes);
            aBuf.order(ByteOrder.nativeOrder());
            ShortBuffer barycentricBuffer = aBuf.asShortBuffer();
            barycentricBuffer.put(baryCoordsArray);
            barycentricBuffer.position(0);
            // baryCoordsArray = null;

            buffers = new int[3];

            //submit to opengl
            GLES30.glGenBuffers(3, buffers, 0);

            //0 - index buf
            //1 - gridpositions buf
            //2 - barycentric coord buf
            //3 - subquad index buf
            GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, buffers[0]);
            GLES30.glBufferData(GLES30.GL_ELEMENT_ARRAY_BUFFER,
                    indexBuffer.capacity() * IntBytes, indexBuffer,
                    GLES30.GL_STATIC_DRAW);
            GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, 0);
            indexBuffer.clear();

            GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, buffers[1]);
            GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, gridPositionsBuffer.capacity() * FloatBytes, gridPositionsBuffer,
                    GLES30.GL_STATIC_DRAW);
            GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0);

            GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, buffers[2]);
            GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, barycentricBuffer.capacity() * ShortBytes, barycentricBuffer,
                    GLES30.GL_STATIC_DRAW);
            GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0);

            indexArraySize = index_array.length;
            uploadedVBO = true;
        }
    }


    public void bindAttributes(GLSLProgram Shader, boolean shadowmapRender) {
        int gridPositionHandle=Shader.getAttributeGLid("a_gridPosition");
        int barycentricHandle=Shader.getAttributeGLid("a_barycentric");

        //grid position buffer
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, buffers[1]);
        GLES30.glVertexAttribPointer(gridPositionHandle, 2, GLES30.GL_FLOAT, false, 0, 0);
        GLES30.glEnableVertexAttribArray(gridPositionHandle);

        //barycentric coords buffer
        if (buffers[2] != -1 && !shadowmapRender) {
            GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, buffers[2]);
            GLES30.glVertexAttribPointer(barycentricHandle, 3, GLES30.GL_SHORT, false, 0, 0);
            GLES30.glEnableVertexAttribArray(barycentricHandle);
        }

        //index buffer
        GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, buffers[0]);
    }

    public void instancedFullmeshDraw(int ninstances)
    {
        GLES30.glDrawElementsInstanced(GLES30.GL_TRIANGLES,indexArraySize,GLES30.GL_UNSIGNED_INT,0,ninstances);
        Singleton.systems.sTime.drawcalls++;
    }

    public void instancedQuarterMeshDraw(int ninstances)
    {
        GLES30.glDrawElementsInstanced(GLES30.GL_TRIANGLES,partialArraySize,GLES30.GL_UNSIGNED_INT,0,ninstances);
        Singleton.systems.sTime.drawcalls++;
    }

    public void draw(boolean[] selection) {
        if (selection[4]) {//the whole node got selected
            GLES30.glDrawElements(GLES30.GL_TRIANGLES, indexArraySize, GLES30.GL_UNSIGNED_INT, 0);
            Singleton.systems.sTime.drawcalls++;

        } else {//only parts of the node got selected (parent covering for it's children)
            for (int j = 0; j < 4; j++) {
                if (selection[j]) {//if  node covers child j's area
                    int offset = offsets[j];
                    int size = partialArraySize;
                    int k = j + 1;

                    while (selection[k] && k < 4) { //consecutive sub-quads will be rendered in one go
                        size += partialArraySize;
                        k++;
                        j++;
                    }
                    GLES30.glDrawElements(GLES30.GL_TRIANGLES, size, GLES30.GL_UNSIGNED_INT, offset);//offset in bytes
                    Singleton.systems.sTime.drawcalls++;
                }
            }
        }
    }

    /**
     * Mark the VBO's as invalid
     */
    public void invalidateVBO() {
        uploadedVBO = false;
    }
}
