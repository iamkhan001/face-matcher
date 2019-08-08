package com.mirobotic.facematcher.ui.activities

import android.content.Context
import android.content.ContextWrapper
import android.graphics.BitmapFactory
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cyberlink.facemedemo.data.SavedFace
import com.mirobotic.facematcher.R
import com.mirobotic.facematcher.ui.adapters.FaceListAdapter
import java.io.File

class FaceListActivity : AppCompatActivity() {

    lateinit var context: Context

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_face_list)
        context = this@FaceListActivity

        getSavedFaces()
    }

    fun getSavedFaces() {

        val savedFaces = ArrayList<SavedFace>()

        val listFile: Array<File>?


        val wrapper = ContextWrapper(context)

        val file = wrapper.getDir("School", Context.MODE_PRIVATE)

        if (file.isDirectory) {
            listFile = file.listFiles()

            if (listFile == null) {
                return
            }

            for (f in listFile) {
                var name = File(f.path).name

                if (name.contains("face_")) {
                    name = name.replace("face_", "")
                }

                if (name.contains(".jpg")) {
                    name = name.replace(".jpg", "")
                }

                savedFaces.add(SavedFace(null, null, null, f.absolutePath, name))
            }
        }

        for (face in savedFaces) {
            try {
                val bitmap = BitmapFactory.decodeFile(face.path) ?: continue
                face.bitmap = bitmap
            } catch (e: Exception) {
                e.printStackTrace()
            }

        }

        if(savedFaces.size>0){
            val rvFaces = findViewById<RecyclerView>(R.id.rvFaces)
            rvFaces.layoutManager = GridLayoutManager(context,2)
            rvFaces.adapter = FaceListAdapter(savedFaces)
        }else{
            Toast.makeText(context,"No Faces Found",Toast.LENGTH_SHORT).show()
        }


    }
}
