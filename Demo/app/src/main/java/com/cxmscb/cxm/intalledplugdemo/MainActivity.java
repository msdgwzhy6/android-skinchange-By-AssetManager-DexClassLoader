package com.cxmscb.cxm.intalledplugdemo;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.Toast;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

import dalvik.system.DexClassLoader;
import dalvik.system.PathClassLoader;

public class MainActivity extends Activity {

    String skinPackageName; //当前皮肤的包名
    RelativeLayout rl;
    SharedPreferences skinType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        rl = (RelativeLayout) findViewById(R.id.relativeLayout);

        skinType = getPreferences(Context.MODE_PRIVATE);
        String skin = skinType.getString("skin", null);
        if (skin != null) installSkin(skin);

    }

    public void changeSkin1(View view) {
        installSkin("dog");
    }

    public void changeSkin2(View view) {
        installSkin("girl");
    }

    public void installSkin(String skinName) {
        String apkPath = findPlugins(skinName);



        if (apkPath == null) {
            // 也可以静默下载皮肤
            Toast.makeText(this, "请先安装皮肤", Toast.LENGTH_SHORT).show();
            // 皮肤插件被删除时，清空存储
            if (skinType.getString("skin", skinName).equals(skinName))
                skinType.edit().clear().commit();
        } else {

            String apkPackageName = "com.cxmscb.cxm."+skinName;
            Resources resources = getSkinApkResource(this,apkPath);
            int bgId = getSkinBackgroundId(apkPath,skinName,apkPackageName);
            rl.setBackgroundDrawable(resources.getDrawable(bgId));
            skinType.edit().putString("skin",skinName).commit();

        }
    }

    private Resources getSkinApkResource(Context context, String apkPath) {
        // 获取加载插件apk的AssetManager
        AssetManager assetManager = createSkinApkAssetManager(apkPath);
        return new Resources(assetManager, context.getResources().getDisplayMetrics(), context.getResources().getConfiguration());
    }

    private AssetManager createSkinApkAssetManager(String apkPath) {
        AssetManager assetManager = null;
        try {
            assetManager = AssetManager.class.newInstance();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        try {
            AssetManager.class.getDeclaredMethod("addAssetPath", String.class).invoke(
                    assetManager, apkPath);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
        return assetManager;
    }


    private int getSkinBackgroundId(String apkPath, String skinName,String apkPackageName) {

        int id = 0;
        try {
            // 在插件R文件中寻找插件资源的id (R也是一个java文件，插件被安装后可以用类加载器PathClassLoader来取得)
           // PathClassLoader pathClassLoader = new PathClassLoader(plugContext.getPackageResourcePath(), ClassLoader.getSystemClassLoader());
            DexClassLoader dexClassLoader = new DexClassLoader(apkPath,this.getDir(skinName,Context.MODE_PRIVATE).getAbsolutePath(),null,this.getClassLoader());
            // 运用反射：
            Class<?> forName = dexClassLoader.loadClass(apkPackageName+".R$drawable");
            // 获取成员变量的值
            for (Field field : forName.getDeclaredFields()) {
                if (field.getName().contains("main_bg")) {
                    id = field.getInt(R.drawable.class);
                    return id;
                }
            }

        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return id;
    }

    private String findPlugins(String plugName) {

        String apkPath = null;

        // 获取apk的路径
        apkPath = Environment.getExternalStorageDirectory()+"/"+ plugName+".apk";

        //皮肤apk存在时，才返回路径
        File file = new File(apkPath);

        if (file.exists()) {

            return apkPath;
        }


        return null;
    }

}