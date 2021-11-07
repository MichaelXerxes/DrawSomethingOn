package com.example.drawsomethingon

import android.app.AlertDialog
import android.app.Dialog
import android.Manifest
import android.app.Instrumentation
//import android.support.v7.app.AppCompatActivity
import android.os.Bundle
//import android.support.v4.content.ContextCompat
import android.view.View
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
//import java.util.jar.Manifest

class MainActivity : AppCompatActivity() {
    private var drawingView: DrawingView? = null
    private var brushbtn: ImageButton? = null
    private var colorbtn: ImageButton? = null
    private var cameraBtn:ImageButton?=null
    private  var colorDialog:Dialog?=null

    private val requestPermission: ActivityResultLauncher<Array<String>> =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()){
            permissions ->
            permissions.entries.forEach{
                val permissionName=it.key

                val isGranted=it.value
                if (isGranted){
                    Toast.makeText(this,
                        "Permission granted now you can read the storage file",
                    Toast.LENGTH_SHORT).show()
                }else{
                    if(permissionName==Manifest.permission.READ_EXTERNAL_STORAGE) {
                        Toast.makeText(
                            this,
                            "Oops you just denied the permission!",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }


    private var mImageBtnCurrentPaintPosition: ImageButton? =null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        drawingView = findViewById(R.id.tv_DrawingView)
        drawingView?.setSizeForBrush(20.toFloat())
        colorbtn = findViewById(R.id.image_btn_colors)
        brushbtn = findViewById(R.id.image_btn_brushview)
        cameraBtn=findViewById(R.id.camerabtn)

        brushbtn!!.setOnClickListener {
            showBrushSizeChooseDialog()
        }
        colorbtn!!.setOnClickListener {
             showColorPalette()
        }
        cameraBtn!!.setOnClickListener {
            requestStoragePermission()
        }
    }


    private fun showBrushSizeChooseDialog() {
        var brushDialog = Dialog(this)
        brushDialog.setContentView(R.layout.dialog_brush_size)
        brushDialog.setTitle("Brush Size")

        val smallbtn: ImageButton = brushDialog.findViewById(R.id.imagebtn_small_brush)
        val mediumbtn: ImageButton = brushDialog.findViewById(R.id.imagebtn_medium_brush)
        val largebtn: ImageButton = brushDialog.findViewById(R.id.imagebtn_large_brush)
        smallbtn.setOnClickListener(View.OnClickListener {
            drawingView?.setSizeForBrush(10.toFloat())
            brushDialog.dismiss()
        })
        mediumbtn.setOnClickListener(View.OnClickListener {
            drawingView?.setSizeForBrush(20.toFloat())
            brushDialog.dismiss()
        })
        largebtn.setOnClickListener(View.OnClickListener {
            drawingView?.setSizeForBrush(30.toFloat())
            brushDialog.dismiss()
        })
        brushDialog.show()

    }

    private fun showColorPalette() {
        colorDialog = Dialog(this)
        colorDialog!!.setContentView(R.layout.dialog_color_rande)
        colorDialog!!.setTitle("Colors")




        colorDialog!!.show()
    }

    fun paintClicked(view: android.view.View) {
        val imageButton=view as ImageButton
        var colorTag=imageButton.tag.toString()
        drawingView?.setColor(colorTag)
        imageButton.setImageDrawable(
            ContextCompat.getDrawable(
                this,R.drawable.selected_pallet_pressed
            )
        )
        mImageBtnCurrentPaintPosition=view
        colorDialog!!.dismiss()

    }
    private fun requestStoragePermission(){
        if(ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE)
        ){
            showRationalDialog("Drawing App !","Drawing App "+
            "needs to Access Your External Storage")
        }else{
            requestPermission.launch(arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE
            //TODO - Add writing external storage permission
            ))
        }

    }
    private fun showRationalDialog(
        title:String,
        message:String
    ){
        val builder:AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setTitle(title)
            .setMessage(message)
            .setPositiveButton("Cancle"){ dialog, _ ->
                dialog.dismiss()
            }
        builder.create().show()
    }
}