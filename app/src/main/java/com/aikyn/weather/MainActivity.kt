@file:Suppress("DEPRECATION")

package com.aikyn.weather

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.aikyn.weather.databinding.ChangeWallpaperDialogBinding
import com.bumptech.glide.Glide
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.net.URL
import kotlin.properties.Delegates.notNull

const val API_KEY = "b12ce30bd3a14bdbb74151713222606"

class MainActivity : AppCompatActivity() {

    private var button: Button? = null
    private var namefield: EditText? = null
    private var info: TextView? = null
    private var prog: TextView? = null
    private var isTrue: Boolean = true
    private var mapTrue: Boolean = true
    private var imageView: ImageView? = null
    private var mapbtn: Button? = null
    private var imageUrl = ""
    private var mapurl = ""
    private var jsoninfo = ""
    private var devbtn: Button? = null
    private var miniInfo: TextView? = null
    private var addButton: Button? = null
    private var pickedPhoto: Uri? = null
    private var pickedBitmap: Bitmap? = null
    private var wallpaper: ImageView? = null
    private var prefs by notNull<SharedPreferences>()

    @SuppressLint("SetTextI18n", "UseCompatLoadingForDrawables")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val instance = savedInstanceState?.getParcelable<Instance>("instance")

        button = findViewById(R.id.button)
        namefield = findViewById(R.id.name_field)
        info = findViewById(R.id.information)
        prog = findViewById(R.id.progress)
        imageView = findViewById(R.id.image)
        mapbtn = findViewById(R.id.map)
        devbtn = findViewById(R.id.devBtn)
        miniInfo = findViewById(R.id.information2)
        wallpaper = findViewById(R.id.wallpaper)
        addButton = findViewById(R.id.add_btn)
        prefs = getSharedPreferences("TABLE", MODE_PRIVATE)

        if (instance != null &&  instance.imageUrl != "") {
            info?.text = instance.info
            miniInfo?.text = instance.miniInfo
            jsoninfo = instance.jsonInfo
            mapurl = instance.mapUrl
            imageUrl = instance.imageUrl
            Glide.with(this).load(imageUrl).into(imageView!!)
        } else if (instance?.imageUrl == "") {
            info?.text = instance.info
            miniInfo?.text = instance.miniInfo
            jsoninfo = instance.jsonInfo
            mapurl = instance.mapUrl
            imageUrl = instance.imageUrl
        }

        while (ContextCompat.checkSelfPermission(this,android.Manifest.permission.READ_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE),
                1)
        }

        loadWallpaperFromInternalStorage()

        button?.setOnClickListener {

            if (!namefield?.text?.toString()?.trim()?.equals("")!! && isTrue) {
                isTrue = false
                val currentOrientation = resources.configuration.orientation

                if (currentOrientation == Configuration.ORIENTATION_PORTRAIT) requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
                if (currentOrientation == Configuration.ORIENTATION_LANDSCAPE) requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE

                val name = namefield!!.text.toString()
                val url = "https://api.weatherapi.com/v1/current.json?key=$API_KEY&q=$name&lang=ru"
                GetResult().execute(url)
            }
        }

        mapbtn?.setOnClickListener {
            if (mapurl != "" && mapTrue) {
                mapTrue = false
                val openmap = Intent(Intent.ACTION_VIEW, Uri.parse(this.mapurl))
                startActivity(openmap)
                Thread {
                    Thread.sleep(1500)
                    mapTrue = true
                }.start()
            }
        }

        devbtn?.setOnClickListener {
            if (jsoninfo != "") {
                val forDev = Intent(this, ForDeveloper::class.java)
                forDev.putExtra("info", jsoninfo)
                startActivity(forDev)
            }
        }

        addButton?.setOnClickListener {
            changeWallpaper()
        }

    }

    fun changeWallpaper() {
        val dialogBinding = ChangeWallpaperDialogBinding.inflate(layoutInflater)
        val builder = AlertDialog.Builder(this).create()

        builder.setView(dialogBinding.root)

        dialogBinding.buttonDefaultWallpaper.setOnClickListener {
            builder.dismiss()
            setDefaultWallpaper()
        }
        dialogBinding.buttonAddNewWallpaper.setOnClickListener {
            builder.dismiss()
            val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(galleryIntent,2)
        }

        builder.show()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable("instance",
            Instance(info?.text.toString(),
                miniInfo?.text.toString(), jsoninfo,
                imageUrl, mapurl))
    }

    private fun loadWallpaperFromInternalStorage() {
        if (currentName() != "null") {
            val file = File("$filesDir/${currentName()}")
            val bytes = file.readBytes()
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)

            Glide.with(this)
                .load(bitmap)
                .into(wallpaper!!)
        }
    }

    private fun setDefaultWallpaper(): Boolean {
        wallpaper?.setImageResource(R.drawable.image_portrait)
        filesDir.listFiles()!!.filter { it.isFile }.map { it.delete() }
        currentName("null")
        return false
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == 2 && resultCode == Activity.RESULT_OK && data != null) {
            pickedPhoto = data.data

            pickedBitmap = if (Build.VERSION.SDK_INT >= 28) {
                val source = ImageDecoder.createSource(this.contentResolver,pickedPhoto!!)
                ImageDecoder.decodeBitmap(source)
            } else {
                MediaStore.Images.Media.getBitmap(this.contentResolver,pickedPhoto)
            }

            val name = getImageFileName(pickedPhoto!!)

            savePhotoToInternalStorage(name, pickedBitmap!!)

            Glide.with(this)
                .load(pickedPhoto!!)
                .into(wallpaper!!)

        super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun savePhotoToInternalStorage(filename: String, bmp: Bitmap): Boolean {
        return try {
            if (filename.endsWith(".jpg") || filename.endsWith(".jpeg")) {
                openFileOutput(filename, MODE_PRIVATE).use { stream ->
                    if(!bmp.compress(Bitmap.CompressFormat.JPEG, 95, stream)) {
                        throw IOException("Couldn't save bitmap.")
                    }
                    currentName(filename)
                }
            } else if (filename.endsWith(".png")) {
                openFileOutput(filename, MODE_PRIVATE).use { stream ->
                    if(!bmp.compress(Bitmap.CompressFormat.PNG, 95, stream)) {
                        throw IOException("Couldn't save bitmap.")
                    }
                    currentName(filename)
                }
            } else if (filename.endsWith(".webp")) {
                openFileOutput(filename, MODE_PRIVATE).use { stream ->
                    if(!bmp.compress(Bitmap.CompressFormat.WEBP, 95, stream)) {
                        throw IOException("Couldn't save bitmap.")
                    }
                    currentName(filename)
                }
            } else {
                throw IOException("Couldn't save bitmap.")
            }

            true
        } catch(e: IOException) {
            e.printStackTrace()
            false
        }

    }

    private fun getImageFileName(uri: Uri): String {
        var path: String? = null
        val proj = arrayOf(MediaStore.Images.Media.DATA)
        val cursor = contentResolver.query(uri, proj, null, null, null)
        if (cursor!!.moveToFirst()) {
            val columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            path = cursor.getString(columnIndex)
        }
        cursor.close()
        return path?.substring(path.lastIndexOf("/").plus(1))!!
    }

    @SuppressLint("StaticFieldLeak")
    inner class GetResult : AsyncTask<String, Void, String>() {
        override fun onPreExecute() {
            super.onPreExecute()
            namefield!!.isEnabled = false
            prog?.text = "Ожидание..."
        }

        override fun doInBackground(vararg p0: String): String {
            val response: String = try {
                URL(p0[0]).readText(Charsets.UTF_8)
            } catch (e: Exception) {
                e.printStackTrace()
                ""
            }
            return response
        }


        //Обработка JSON
        @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
        @SuppressLint("SetTextI18n")
        override fun onPostExecute(response: String?) {
            super.onPostExecute(response)
            try {
                val jsonObject = JSONObject(response)
                this@MainActivity.jsoninfo = jsonObject.toString()
                val temp: String = jsonObject.getJSONObject("current").getString("temp_c")
                val cond: String = jsonObject.getJSONObject("current").getJSONObject("condition").getString("text")
                val name: String = jsonObject.getJSONObject("location").getString("name")
                val country: String = jsonObject.getJSONObject("location").getString("country")
                val lat: String = jsonObject.getJSONObject("location").getString("lat")
                val lon: String = jsonObject.getJSONObject("location").getString("lon")
                val humidity: String = jsonObject.getJSONObject("current").getString("humidity")
                val wind: String = jsonObject.getJSONObject("current").getString("wind_kph")
                val precip: String = jsonObject.getJSONObject("current").getString("precip_mm")
                val cloud: String = jsonObject.getJSONObject("current").getString("cloud")


                imageUrl = "https:" + jsonObject.getJSONObject("current").getJSONObject("condition").getString("icon")
                mapurl = "https://yandex.kz/pogoda/maps/nowcast?/z=9&lat=$lat&lon=$lon"
                Glide.with(this@MainActivity).load(imageUrl).into(imageView!!)
                try {
                    val time = jsonObject.getJSONObject("location").getString("localtime").substring(11, 16)
                    info!!.text = "В городе $name из страны $country сейчас $time\n" +
                            "Температура: $temp\n$cond"
                } catch (e: IndexOutOfBoundsException) {
                    val time = jsonObject.getJSONObject("location").getString("localtime").substring(11, 15)
                    info!!.text = "В городе $name из страны $country сейчас $time\n" +
                            "Температура: $temp\n$cond"
                }

                miniInfo!!.text =   "Влажность: $humidity%\n" +
                                    "Скорость ветра, км/ч: $wind\n" +
                                    "Осадки, мм: $precip\n" +
                                    "Облачность: $cloud%"

            } catch (e: JSONException) {

                e.printStackTrace()
                info!!.text = "Вы что-то ввели неправильно или интернет не включен!"
                miniInfo!!.text = ""
                imageView?.setImageResource(R.drawable.x)
                jsoninfo = ""
                mapurl = ""

            } finally {
                prog?.text = ""
                namefield!!.isEnabled = true
                isTrue = true
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            }
        }

    }

    private fun currentName(filename: String? = null): String? {
        if (filename == null) {
            return prefs.getString("name", "null")
        } else {
            prefs.edit().apply {
                putString("name", filename)
                apply()
            }
            return null
        }
    }

}