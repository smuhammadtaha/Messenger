package com.example.kotlinmessenger.messages

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.example.kotlinmessenger.R
import com.example.kotlinmessenger.models.ChatMessage
import com.example.kotlinmessenger.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.squareup.picasso.Picasso
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import com.xwray.groupie.Item
import kotlinx.android.synthetic.main.activity_chat_log.*
import kotlinx.android.synthetic.main.chat_from_row.view.*
import kotlinx.android.synthetic.main.chat_to_row.view.*

class ChatLogActivity : AppCompatActivity() {

    companion object{
        val TAG = "ChatLog"
    }

    val adapter=GroupAdapter<GroupieViewHolder>()

    var toUser:User?=null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat_log)

        recyclerview_chatlog.adapter=adapter

        //val username=intent.getStringExtra(NewMessageActivity.USER_KEY)
        toUser=intent.getParcelableExtra<User>(NewMessageActivity.USER_KEY)
        supportActionBar?.title=toUser?.username

       // setupDummyData()
      listenForMessages()

        send_btn.setOnClickListener {
            Log.d(TAG,"Attempt to send message...")
            performSendMessage()
        }
    }
    private fun listenForMessages(){
        val fromId=FirebaseAuth.getInstance().uid
        val toId=toUser?.uid
        val ref=FirebaseDatabase.getInstance().getReference("/user-messages/$fromId/$toId")

        ref.addChildEventListener(object : ChildEventListener{
            override fun onChildAdded(p0: DataSnapshot, p1: String?) {
                val chatMessage=p0.getValue(ChatMessage::class.java)

                if(chatMessage != null)
                {
                    Log.d(TAG,chatMessage.text)

                    if(chatMessage.fromId==FirebaseAuth.getInstance().uid)
                    {
                        val currentUser=LatestMessageActivity.currentUser ?:return
                        adapter.add(ChatToitem(chatMessage.text,currentUser))
                    }
                    else
                    {
                        adapter.add(ChatFromitem(chatMessage.text,toUser!!))
                    }
                }
                recyclerview_chatlog.scrollToPosition(adapter.itemCount - 5)
            }

            override fun onCancelled(p0: DatabaseError) {

            }

            override fun onChildChanged(p0: DataSnapshot, p1: String?) {

            }

            override fun onChildMoved(p0: DataSnapshot, p1: String?) {

            }

            override fun onChildRemoved(p0: DataSnapshot) {

            }
        })
    }


    private fun performSendMessage(){
        val text =edittext_chatlog.text.toString()

        val fromId=FirebaseAuth.getInstance().uid
        val user=intent.getParcelableExtra<User>(NewMessageActivity.USER_KEY)
        val toId=user.uid

        if(fromId==null)return

//        val reference=FirebaseDatabase.getInstance().getReference("/message").push()
        val reference=FirebaseDatabase.getInstance().getReference("/user-messages/$fromId/$toId").push()
        val toReference=FirebaseDatabase.getInstance().getReference("/user-messages/$toId/$fromId").push()


        val chatMessage=
            ChatMessage(
                reference.key!!,
                text,
                fromId,
                toId,
                System.currentTimeMillis() / 1000
            )
        reference.setValue(chatMessage)
            .addOnSuccessListener {
                Log.d(TAG,"Saved our chat message: ${reference.key}")
                edittext_chatlog.text.clear()
                recyclerview_chatlog.scrollToPosition(adapter.itemCount-1)
            }
        toReference.setValue(chatMessage)

        val latestMsgsRef=FirebaseDatabase.getInstance().getReference("/latest-messages/$fromId/$toId")
        latestMsgsRef.setValue(chatMessage)

        val latestMsgsToRef=FirebaseDatabase.getInstance().getReference("/latest-messages/$toId/$fromId")
        latestMsgsToRef.setValue(chatMessage)
    }

}
class ChatFromitem(val text:String, val user:User):Item<GroupieViewHolder>(){
    override fun bind(viewHolder: GroupieViewHolder, position: Int) {
        viewHolder.itemView.textView_fromrow.text=text

        val uri=user.profileImageUrl
        val targetImageView=viewHolder.itemView.imageView_fromchat
        Picasso.get().load(uri).into(targetImageView)
    }

    override fun getLayout(): Int {
        return R.layout.chat_from_row
    }
}
class ChatToitem(val text:String, val user:User):Item<GroupieViewHolder>(){
    override fun bind(viewHolder: GroupieViewHolder, position: Int) {
        viewHolder.itemView.textView_torow.text=text

        //load our user image into the star
        val uri=user.profileImageUrl
        val targetImageView=viewHolder.itemView.imageView_tochat
        Picasso.get().load(uri).into(targetImageView)
    }

    override fun getLayout(): Int {
        return R.layout.chat_to_row
    }
}
