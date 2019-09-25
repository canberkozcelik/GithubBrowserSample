package com.co.example.github.ui.common

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingComponent
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.DiffUtil
import com.co.example.github.AppExecutors
import com.co.example.github.R
import com.co.example.github.databinding.RepoItemBinding
import com.co.example.github.vo.UserRepo


/**
 * A RecyclerView adapter for [UserRepo] class.
 */
class UserRepoListAdapter(
        private val dataBindingComponent: DataBindingComponent,
        appExecutors: AppExecutors,
        private val showFullName: Boolean,
        private val repoClickCallback: ((UserRepo) -> Unit)?
) : DataBoundListAdapter<UserRepo, RepoItemBinding>(
        appExecutors = appExecutors,
        diffCallback = object : DiffUtil.ItemCallback<UserRepo>() {
            override fun areItemsTheSame(oldItem: UserRepo, newItem: UserRepo): Boolean {
                return oldItem.owner == newItem.owner
                        && oldItem.name == newItem.name
            }

            override fun areContentsTheSame(oldItem: UserRepo, newItem: UserRepo): Boolean {
                return oldItem.description == newItem.description
                        && oldItem.stars == newItem.stars
            }
        }
) {

    override fun createBinding(parent: ViewGroup): RepoItemBinding {
        val binding = DataBindingUtil.inflate<RepoItemBinding>(
                LayoutInflater.from(parent.context),
                R.layout.repo_item,
                parent,
                false,
                dataBindingComponent
        )
        binding.showFullName = showFullName
        binding.root.setOnClickListener {
            binding.repo?.let {
                repoClickCallback?.invoke(it)
            }
        }
        return binding
    }

    override fun bind(binding: RepoItemBinding, item: UserRepo) {
        binding.repo = item
        binding.imgFav.isSelected = binding.repo!!.favorite
    }
}