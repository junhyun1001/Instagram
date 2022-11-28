package com.example.instagram

import android.graphics.PorterDuff
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.*
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.instagram.databinding.FragmentYourProfileBinding
import com.example.instagram.model.AlarmDTO
import com.example.instagram.model.ContentDTO
import com.example.instagram.model.FollowDTO
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class YourProfileFragment :
    BaseFragment<FragmentYourProfileBinding>(R.layout.fragment_your_profile) {
    var firestore: FirebaseFirestore? = null
    var auth: FirebaseAuth? = null
    var uid: String? = null
    var currentUserUid: String? = null
    var contentDTOs: ArrayList<ContentDTO> = arrayListOf()
    var destinationUid: String? = null

    var followListenerRegistration: ListenerRegistration? = null
    var followingListenerRegistration: ListenerRegistration? = null

    var userId: String? = null


    override fun initStartView() {
        super.initStartView()
        (activity as MainActivity).hideNav()

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        currentUserUid = auth?.currentUser?.uid

        getFollowerAndFollowing()

    }

    override fun initDataBinding() {
        super.initDataBinding()

        binding.accountBtnFollow.setOnClickListener {
            requestFollow()
        }

        binding.accountRecyclerview.adapter = UserFragmentRecyclerViewAdapter()
        binding.accountRecyclerview.layoutManager = GridLayoutManager(context, 3)

    }

    fun getFollowerAndFollowing() {
        setFragmentResultListener("userId") { _, bundle ->
            userId = bundle.getString("DTOsUserId")
            binding.username.text = userId
            setFragmentResultListener("destinationUid") { _, bundle ->
                // 여기서 binding.textview해서 아이디 바꿔야됨
                binding.username.text = userId
                destinationUid = bundle.getString("DTOsUid")
                uid = destinationUid

                setFragmentResult(
                    "uid",
                    bundleOf("uid" to uid)
                )

                firestore?.collection("users")?.document(uid!!)
                    ?.addSnapshotListener { documentSnapshot, firebaseFirestoreException ->
                        if (documentSnapshot == null) return@addSnapshotListener
                        var followDTO = documentSnapshot.toObject(FollowDTO::class.java)

                        if (followDTO?.followingCount != null) {
                            binding.accountTvFollowingCount.text =
                                followDTO.followingCount.toString()
                            // currentUserUid: 현재 접속된 계정
                            // uid: 상대방 계정
                        }
                        if (followDTO?.followerCount != null) {
                            binding.accountTvFollowCount.text = followDTO.followerCount.toString()
                        }

                        if (followDTO != null) {
                            if (followDTO.followers[currentUserUid] == true) {
                                binding.accountBtnFollow.text = "언팔로우하기"
                            } else
                                binding.accountBtnFollow.text = "팔로우하기"
                        }
                    }
            }
        }
    }


    // 내 계정의 데이터 저장
// 상대방 계정의 데이터 저장
    fun requestFollow() {
        // 내 계정에 데이터 저장
        var tsDocFollowing = firestore!!.collection("users").document(currentUserUid!!)
        firestore?.runTransaction { transaction ->
            var followDTO = transaction.get(tsDocFollowing).toObject(FollowDTO::class.java)
            if (followDTO == null) {
                followDTO = FollowDTO()
                followDTO.followingCount = 1
                followDTO.followings[uid!!] = true

                transaction.set(tsDocFollowing, followDTO)
                return@runTransaction
            }

            // 내가 팔로우 한 상태일 때(언팔로우 버튼이 떠 있어야 함)
            if (followDTO.followings.containsKey(uid)) {
                followDTO.followingCount = followDTO.followingCount - 1
                followDTO.followings.remove(uid)

            } else { // 내가 팔로우 하지 않은 상태(팔로우 버튼이 떠 있어야 함)
                followDTO.followingCount = followDTO.followingCount + 1
                followDTO.followings[uid!!] = true
                followerAlarm(uid!!)
            }
            transaction.set(tsDocFollowing, followDTO)
            return@runTransaction
        }

        // 상대방 계정에 데이터 저장
        var tsDocFollower = firestore!!.collection("users").document(uid!!)
        firestore?.runTransaction { transaction ->

            var followDTO = transaction.get(tsDocFollower).toObject(FollowDTO::class.java)
            if (followDTO == null) {

                followDTO = FollowDTO()
                followDTO!!.followerCount = 1
                followDTO!!.followers[currentUserUid!!] = true

                transaction.set(tsDocFollower, followDTO!!)
                followerAlarm(uid!!)

                return@runTransaction
            }

            // 내가 상대방 계정에 팔로우를 했을 경우
            if (followDTO?.followers?.containsKey(currentUserUid!!)!!) {
                followDTO!!.followerCount = followDTO!!.followerCount - 1
                followDTO!!.followers.remove(currentUserUid!!)

            } else { // 상대방 계정을 팔로우 하지 않았을 경우
                followDTO!!.followerCount = followDTO!!.followerCount + 1
                followDTO!!.followers[currentUserUid!!] = true
                followerAlarm(uid!!)
            }// Star the post and add self to stars

            transaction.set(tsDocFollower, followDTO!!)
            return@runTransaction
        }
    }

    fun followerAlarm(destinationUid: String) {

        val alarmDTO = AlarmDTO()
        alarmDTO.destinationUid = destinationUid
        alarmDTO.userId = auth?.currentUser!!.email
        alarmDTO.uid = auth?.currentUser!!.uid
        alarmDTO.kind = 2
        alarmDTO.timestamp = System.currentTimeMillis()

        FirebaseFirestore.getInstance().collection("alarms").document().set(alarmDTO)
//        var message = auth?.currentUser!!.email + getString(R.string.alarm_follow)
//        fcmPush?.sendMessage(destinationUid, "알림 메세지 입니다.", message)
    }


    inner class UserFragmentRecyclerViewAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        var contentDTOs: ArrayList<ContentDTO> = ArrayList()


        init {
            setFragmentResultListener("uid") { _, bundle ->
                uid = bundle.getString("uid")
                FirebaseFirestore
                    .getInstance().collection("images").whereEqualTo("uid", uid)
                    .addSnapshotListener { querySnapshot, firebaseFirestore ->
                        if (querySnapshot == null) return@addSnapshotListener
                        for (snapshot in querySnapshot.documents) {
                            contentDTOs.add(snapshot.toObject(ContentDTO::class.java)!!)
                        }
                        binding.accountTvPostCount.text = contentDTOs.size.toString()
                        notifyDataSetChanged()
                    }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            var width = resources.displayMetrics.widthPixels / 3

            var imageview = ImageView(parent.context)
            imageview.layoutParams = LinearLayoutCompat.LayoutParams(width, width)
            return CustomViewHolder(imageview)

        }

        inner class CustomViewHolder(var imageview: ImageView) :
            RecyclerView.ViewHolder(imageview)


        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            var imageview = (holder as CustomViewHolder).imageview


            Glide.with(holder.itemView.context)
                .load(contentDTOs[position].imageUrl)
                .apply(RequestOptions().centerCrop())
                .into(imageview)
        }

        override fun getItemCount(): Int {
            return contentDTOs.size
        }

    }


    override fun initAfterBinding() {
        super.initAfterBinding()


    }

}