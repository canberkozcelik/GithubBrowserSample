package com.co.example.github.ui.userrepo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingComponent
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.findNavController
import androidx.transition.TransitionInflater
import com.co.example.github.AppExecutors
import com.co.example.github.testing.OpenForTesting
import com.co.example.github.R
import com.co.example.github.binding.FragmentDataBindingComponent
import com.co.example.github.databinding.UserRepoFragmentBinding
import com.co.example.github.di.Injectable
import com.co.example.github.ui.common.BackCallback
import com.co.example.github.ui.common.FavCallback
import com.co.example.github.ui.common.RetryCallback
import com.co.example.github.util.autoCleared
import com.co.example.github.vo.UserRepo
import javax.inject.Inject

@OpenForTesting
class UserRepoFragment : Fragment(), Injectable {
    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    lateinit var userRepoViewModel: UserRepoViewModel

    @Inject
    lateinit var appExecutors: AppExecutors

    lateinit var repo : UserRepo

    var binding by autoCleared<UserRepoFragmentBinding>()
    var dataBindingComponent: DataBindingComponent = FragmentDataBindingComponent(this)

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        val dataBinding = DataBindingUtil.inflate<UserRepoFragmentBinding>(
                inflater,
                R.layout.user_repo_fragment,
                container,
                false,
                dataBindingComponent
        )
        dataBinding.retryCallback = object : RetryCallback {
            override fun retry() {
                userRepoViewModel.retry()
            }
        }
        dataBinding.favCallback = object : FavCallback {
            override fun favorite() {
                binding.imgFav.isSelected = !binding.imgFav.isSelected
                repo.favorite = !repo.favorite
            }
        }
        dataBinding.backCallback = object : BackCallback {
            override fun back() {
                findNavController().popBackStack()
            }
        }
        binding = dataBinding
        sharedElementReturnTransition = TransitionInflater.from(context).inflateTransition(R.transition.move)
        return dataBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        userRepoViewModel = ViewModelProviders.of(this, viewModelFactory)
                .get(UserRepoViewModel::class.java)
        val params = UserRepoFragmentArgs.fromBundle(arguments!!)
        repo = params.repo
        userRepoViewModel.setId(repo.owner.login, repo.name)
        binding.lifecycleOwner = viewLifecycleOwner
        binding.userRepo = userRepoViewModel.repo
        binding.imgFav.isSelected = repo.favorite
        postponeEnterTransition()
        binding.description.viewTreeObserver.addOnPreDrawListener {
            startPostponedEnterTransition()
            true
        }
    }
}
