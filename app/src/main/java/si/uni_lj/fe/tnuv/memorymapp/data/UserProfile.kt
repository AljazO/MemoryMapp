package si.uni_lj.fe.tnuv.memorymapp.data

data class UserProfile(
    val uid: String = "",
    val fullName: String = "",
    val email: String = "",
    val username: String = "",
    val bio: String = "",
    val profilePictureUrl: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
