package com.sdgapps.terrainsandbox.MiniEngine.graphics.glsl;

import android.content.res.AssetManager;
import android.opengl.GLES30;

import com.sdgapps.terrainsandbox.utils.Logger;

import java.util.HashMap;
import java.util.Map;

/**
 * GLSL Program encapsulates the vertex and the fragment shader interface.
 *
 * No render data should be stored here, this class is merely an interface allowing
 * users to interact with the OpenGL Shader it represents.
 *
 * ShaderUniform subclasses are used to pass uniform data to the actual shaders.
 *
 */
public class GLSLProgram {

    private HashMap<String, ShaderUniform> uniforms = new HashMap<>();
    private HashMap<String, Integer> attributes=new HashMap<>();

    /**
     * Keeps track of the amount of samplers in the shader, in order to
     * set the active texture (glActiveTexture) properly for every sampler in the shader.
     *
     * The shader's samplers will be sequentially assigned to active texture slots.
     */
    private int nsamplers=0;

    /** OpenGL handle of this program*/
    public int glHandle = -1;

    private GLSLShader vertex, fragment;

    /**Shader identifier in the engine*/
    String shaderID;


    public GLSLProgram(String id, String vertexPath, String fragmentPath, ShaderSystem shaderSys,boolean optimize) {
        AssetManager am =shaderSys.getAssetMngr();
        this.shaderID = id;
        vertex = new GLSLShader(vertexPath, am, false,id,optimize);
        fragment = new GLSLShader(fragmentPath, am, true,id,optimize);

        glHandle = createAndLinkProgram(vertex.glHandle,
                fragment.glHandle, new String[]{
                        "a_Position", "a_Normal"
                });
        buildVariables();
    }

    /**
     * Useful for context changes when android destroys the egl context (shaders, textures etc. are
     * removed from GPU memory)
     */
    public void reloadToGPU(AssetManager am) {
        this.vertex.reloadShader(am);
        this.fragment.reloadShader(am);
        int vertexhandle = this.vertex.glHandle;
        int fragmenthandle = this.fragment.glHandle;

        glHandle = createAndLinkProgram(vertexhandle,
                fragmenthandle, new String[]{
                        "a_Position", "a_Normal"
                });
        buildVariables();
        rebuildUserVariables();
    }

    private void rebuildUserVariables() {
        this.buildVariables();

        for (ShaderUniform sv : uniforms.values()) {
            sv.glHandle = GLES30.glGetUniformLocation(glHandle, sv.name);
        }
    }

    void buildVariables() {
        /**NOTE: glGetUniformLocation and glGetAttribLocation are allowed to return -1 if the uniform//attribute is not used by the
         * shader, even if it is declared.*/
        //update attrib location
        for (Map.Entry<String, Integer> entry  : attributes.entrySet())
        {
            entry.setValue(GLES30.glGetAttribLocation(glHandle,entry.getKey()));
        }
    }

    public int getAttributeGLid(String name)
    {
        Integer glid = attributes.get(name);

        if(glid==null)
            return -1;
        else
            return glid;
    }

    public int linkAttribute(String nameInShader)
    {
        int glid=GLES30.glGetAttribLocation(glHandle, nameInShader);
        if(glid!=-1)
            attributes.put(nameInShader, glid);

        return glid;
    }

    public void useProgram() {
        GLES30.glUseProgram(glHandle);
    }


    public void addUniform(ShaderUniform sv) {

        if(sv instanceof Sampler)
        {
            ((Sampler) sv).activeTarget=nsamplers;
            nsamplers++;
        }

        sv.glHandle = GLES30.glGetUniformLocation(glHandle, sv.name);
        uniforms.put(sv.name, sv);
    }

    public ShaderUniform getUniform(String uname) {
        ShaderUniform res = uniforms.get(uname);
        return res;
    }

    /**
     * Compiles and links a Shader Program
     *
     * @param vertexShaderHandle   Vertex shader OpenGL Handle
     * @param fragmentShaderHandle Fragment shader OpenGL Handle
     * @param attributes           Program attributes
     * @return OpenGL handle of the program
     */
    private int createAndLinkProgram(final int vertexShaderHandle,
                                            final int fragmentShaderHandle, final String[] attributes) {
        int programHandle = GLES30.glCreateProgram();

        if (programHandle != 0) {
            //Bind the vertex shader to the program.
            GLES30.glAttachShader(programHandle, vertexShaderHandle);

            //Bind the fragment shader to the program.
            GLES30.glAttachShader(programHandle, fragmentShaderHandle);

            //Bind attributes
            if (attributes != null) {
                final int size = attributes.length;
                for (int i = 0; i < size; i++) {
                    GLES30.glBindAttribLocation(programHandle, i, attributes[i]);
                }
            }

            //Link the two shaders together into a program.
            GLES30.glLinkProgram(programHandle);

            //Get the link status.
            final int[] linkStatus = new int[1];
            GLES30.glGetProgramiv(programHandle, GLES30.GL_LINK_STATUS, linkStatus, 0);

            //If the link failed, delete the program.
            if (linkStatus[0] == 0) {
                Logger.err("Error linking program: "+shaderID+" " + GLES30.glGetProgramInfoLog(programHandle));
                GLES30.glDeleteProgram(programHandle);
                programHandle = 0;
                throw new RuntimeException("Error linking program: "+shaderID+" " + GLES30.glGetProgramInfoLog(programHandle));
            }
            else
                Logger.log("GLes linking log for "+shaderID+" " + GLES30.glGetProgramInfoLog(programHandle));
        }

        if (programHandle == 0) {
            throw new RuntimeException("Error creating program.");
        }

        return programHandle;
    }

    public String toString() {
        return " vertexHandle: " + this.vertex.glHandle + " fragmentHandle: " + this.fragment.glHandle;
    }
}
