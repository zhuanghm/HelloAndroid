package com.ihemingway.helloworld

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import com.ihemingway.helloworld.filereader.FileReadListActivity
import com.ihemingway.helloworld.filereader.FileReaderActivity
import com.ihemingway.helloworld.filereader.ReadOfficeActivity
import com.ihemingway.helloworld.shortcut.ShortCutActivity
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Example of a call to a native method
//        tvHello.text = stringFromJNI()
        tvHello.setOnClickListener {
            startActivity1()
        }
    }

    fun startActivity1(){
        var intent = Intent(Intent.ACTION_CREATE_SHORTCUT)
        intent.setClass(this, FileReadListActivity::class.java)
//        intent.setAction()
        startActivityForResult(intent,100)
    }
    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    external fun stringFromJNI(): String

//    companion object {
//
//        // Used to load the 'native-lib' library on application startup.
//        init {
//            System.loadLibrary("native-lib")
//        }
//    }
}
