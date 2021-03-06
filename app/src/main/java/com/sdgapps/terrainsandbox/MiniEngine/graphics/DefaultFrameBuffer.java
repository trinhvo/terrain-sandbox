package com.sdgapps.terrainsandbox.MiniEngine.graphics;

import android.opengl.GLES30;

import com.sdgapps.terrainsandbox.GLSurfaceRenderer;

public class DefaultFrameBuffer implements FrameBufferInterface {
    @Override
    public void bind() {
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0);

        //Recover the standard viewport
        GLES30.glViewport(0, 0, GLSurfaceRenderer.surface_width, GLSurfaceRenderer.surface_height);

        //setup the culling back to GL_BACK
        GLES30.glCullFace(GLES30.GL_BACK);
        GLES30.glColorMask(true, true, true, true);
        GLES30.glClearColor(0, 0, 0, 1);
    }

    @Override
    public void setup() {
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0);
    }
}
