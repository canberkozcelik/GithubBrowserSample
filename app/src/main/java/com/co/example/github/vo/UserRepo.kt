package com.co.example.github.vo

import android.os.Parcelable
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize

@Entity(
        indices = [
            Index("id"),
            Index("owner_login")],
        primaryKeys = ["name", "owner_login"]
)
@Parcelize
data class UserRepo(
        @field:SerializedName("id")
        val id: Int,
        @field:SerializedName("name")
        val name: String,
        @field:SerializedName("full_name")
        val fullName: String,
        @field:SerializedName("description")
        val description: String?,
        @field:SerializedName("owner")
        @field:Embedded(prefix = "owner_")
        val owner: Owner,
        @field:SerializedName("stargazers_count")
        val stars: Int,
        @field:SerializedName("open_issues_count")
        val openIssuesCount: Int,
        @field:SerializedName("favorite")
        var favorite: Boolean
) : Parcelable {

    @Parcelize
    data class Owner(
            @field:SerializedName("login")
            val login: String,
            @field:SerializedName("url")
            val url: String?,
            @field:SerializedName("avatar_url")
            val avatarUrl: String?
    ) : Parcelable

    companion object {
        const val UNKNOWN_ID = -1
    }

}