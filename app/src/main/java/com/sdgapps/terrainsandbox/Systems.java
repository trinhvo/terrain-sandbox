package com.sdgapps.terrainsandbox;

import com.sdgapps.terrainsandbox.MiniEngine.TimeSystem;
import com.sdgapps.terrainsandbox.MiniEngine.behaviours.Camera;
import com.sdgapps.terrainsandbox.MiniEngine.behaviours.Light;
import com.sdgapps.terrainsandbox.MiniEngine.graphics.glsl.ShaderSystem;
import com.sdgapps.terrainsandbox.MiniEngine.graphics.texture.TextureManager;

public class Systems {

    public ShaderSystem sShaderSystem = new ShaderSystem();
    public TimeSystem sTime = new TimeSystem();
    public TextureManager textureManager=new TextureManager();

    public Camera mainCamera;
    public Light mainLight;
}
