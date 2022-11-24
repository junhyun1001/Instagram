package com.example.hanstargram.navigation.model

data class FollowDTO (
    var followerCount : Int = 0,
    var followers : MutableMap<String, Boolean> = HashMap(),
    var followingCount : Int = 0,
    var followings : MutableMap<String, Boolean> = HashMap()

//package com.example.instagram.model
//
//import java.util.HashMap
//
//data class FollowDTO(
//
//    var followerCount: Int = 0,
//    var followers: MutableMap<String, Boolean> = HashMap(),
//
//    var followingCount: Int = 0,
//    var followings: MutableMap<String, Boolean> = HashMap()
)