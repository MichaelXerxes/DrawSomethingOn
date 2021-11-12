package com.example.drawsomethingon

import android.app.AlertDialog
import android.app.Dialog
import android.Manifest
import android.app.Instrumentation
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Color.WHITE
import android.media.MediaScannerConnection
//import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
//import android.support.v4.content.ContextCompat
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.lang.Exception

//import java.util.jar.Manifest

class MainActivity : AppCompatActivity() {
    private var drawingView: DrawingView? = null
    private var brushbtn: ImageButton? = null
    private var colorbtn: ImageButton? = null
    private var cameraBtn:ImageButton?=null
    private var clearBtn:ImageButton?=null
    private var saveBtn:ImageButton?=null
    private var colorDialog:Dialog?=null
    private var customProgressDialog:Dialog?=null

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
                    val pickIntent=Intent(Intent.ACTION_PICK,
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                    openGalleryLauncher.launch(pickIntent)

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

    private val openGalleryLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
            result ->
            if(result.resultCode == RESULT_OK && result.data!=null){
                val imageBackGround:ImageView = findViewById(R.id.iv_background)
                //that will set image
                imageBackGround.setImageURI(result.data?.data)
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
        clearBtn=findViewById(R.id.clearbtn)
        saveBtn=findViewById(R.id.savebtn)

        brushbtn!!.setOnClickListener {
            showBrushSizeChooseDialog()
        }
        colorbtn!!.setOnClickListener {
             showColorPalette()
        }
        cameraBtn!!.setOnClickListener {
            requestStoragePermission()
        }
        clearBtn!!.setOnClickListener {
            drawingView?.onClickUndo()
            Toast.makeText(
                this@MainActivity,
                "Back !!",
                Toast.LENGTH_SHORT).show()

        }
        saveBtn!!.setOnClickListener {
            if (isReadStorageAllowed()){
                showProgressDialog()
                lifecycleScope.launch {
                    val flDrawingView:FrameLayout=findViewById(R.id.frmal_drawing_view_container)
                    val myBitMap:Bitmap=getBitmapFromView(flDrawingView)
                    saveBitMapFile(myBitMap)
                }
            }

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
    private fun getBitmapFromView(view: View):Bitmap{
        val  returnBitMap=Bitmap.createBitmap(view.width,view.height,
            Bitmap.Config.ARGB_8888)
        val canvas= Canvas(returnBitMap)
        val backgrdDrawable=view.background
        if(backgrdDrawable!=null){
            backgrdDrawable.draw(canvas)
        }else{
            canvas.drawColor(Color.WHITE)
        }
        view.draw(canvas)


        return  returnBitMap
    }

    private suspend fun saveBitMapFile(mBitmap:Bitmap?):String{
        var result=""
        withContext(Dispatchers.IO){
            if(mBitmap!=null){
                try {
                    val bytes= ByteArrayOutputStream()
                    //format   .png
                    mBitmap.compress(Bitmap.CompressFormat.PNG,90,bytes)

                    val f =File(externalCacheDir?.absoluteFile.toString()
                            + File.separator + "DrawSomethingOn_"+System.currentTimeMillis()/1000+".png")

                    val fo=FileOutputStream(f)

                    fo.write((bytes.toByteArray()))
                    fo.close()

                    result=f.absolutePath

                    runOnUiThread {
                        cancelProgressDialog()
                        if (result.isNotEmpty()){
                            Toast.makeText(this@MainActivity,
                            "File saved successfully :$result",Toast.LENGTH_SHORT
                            ).show()
                            shareImage(result)
                        }else{
                            Toast.makeText(this@MainActivity,
                                "Something went wrong",Toast.LENGTH_SHORT
                            ).show()
                        }
                    }

                }catch (e:Exception){
                    result=""

                    e.printStackTrace()
                }
            }
        }
        return result
    }

    private fun isReadStorageAllowed():Boolean{
        val result=ContextCompat.checkSelfPermission(this,
            Manifest.permission.READ_EXTERNAL_STORAGE)
        //if we get 0 in result and Permission granted is also 0 then return true
        return result==PackageManager.PERMISSION_GRANTED
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
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
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

    private fun showProgressDialog(){
        customProgressDialog= Dialog(this@MainActivity)

        customProgressDialog?.setContentView(R.layout.dialog_custom_progress)

        customProgressDialog?.show()
    }

    private fun cancelProgressDialog(){
        if (customProgressDialog!=null){
            customProgressDialog?.dismiss()
            customProgressDialog=null
        }
    }

    private fun shareImage(result:String){
        MediaScannerConnection.scanFile(this,
        arrayOf(result),null){
            path,uri ->
            val shareIntent= Intent()
            shareIntent.action=Intent.ACTION_SEND
            shareIntent.putExtra(Intent.EXTRA_STREAM,uri)
            shareIntent.type="image/png"
            startActivity(Intent.createChooser(shareIntent,"Share"))


        }
    }
}