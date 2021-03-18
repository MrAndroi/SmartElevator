package com.shorman.smartelevator

import android.content.Context
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.Network
import android.net.NetworkRequest
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.navigation.fragment.NavHostFragment
import androidx.room.Update
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.ktx.Firebase
import com.shorman.smartelevator.ui.viewModels.MainViewModel
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*

class MainActivity : AppCompatActivity() {

    //get a reference for view model using ktx library
    private val viewModel by viewModels<MainViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        startNetworkCallback()

        //check if the user is not authticated and open login fragment
        if(Firebase.auth.currentUser != null){
                val navHostFragment = navHostFragment as NavHostFragment
                val inflater = navHostFragment.navController.navInflater
                val graph = inflater.inflate(R.navigation.nav_graph)
                graph.startDestination = R.id.mainScreenFragment
                navHostFragment.navController.graph = graph
        }
        else{
                val navHostFragment = navHostFragment as NavHostFragment
                val inflater = navHostFragment.navController.navInflater
                val graph = inflater.inflate(R.navigation.nav_graph)
                graph.startDestination = R.id.loginFragment
                navHostFragment.navController.graph = graph
        }

        //observe network status via isNetworkAvailable and show user error when the value is false
        viewModel.isNetworkAvailable.observe(this){
            if(it) {
                hideNoInternet()
            }
            else{
                showNoInternet()
            }
        }

    }

    //update app language when the base attached to context
    override fun attachBaseContext(newBase: Context?) {
        val prefs = newBase!!.getSharedPreferences(
                "language",
                MODE_PRIVATE
        )
        val localeString:String? =
                prefs.getString("lang", "en")
        val myLocale = Locale(localeString!!)
        Locale.setDefault(myLocale)
        val config = newBase.resources.configuration
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocale(myLocale)
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.N) {
                val newContext = newBase.createConfigurationContext(config)
                super.attachBaseContext(newContext)
                return
            }
        } else {
            config.locale = myLocale
        }
        super.attachBaseContext(newBase)
        resources.updateConfiguration(config, resources.displayMetrics)
    }

    // function to get network status in live using NetworkCallback() and update user interface when the connection lost
    private fun startNetworkCallback() {
        val cm: ConnectivityManager =
                application.getSystemService(Context.CONNECTIVITY_SERVICE)  as ConnectivityManager
        val builder: NetworkRequest.Builder = NetworkRequest.Builder()

        cm.registerNetworkCallback(
                builder.build(),
                object : ConnectivityManager.NetworkCallback() {

                    override fun onAvailable(network: Network) {
                        //when the network is available update isNetworkAvailable value to true in viewModel so we can observe
                        //on it any where
                        CoroutineScope(Dispatchers.Main).launch {
                            viewModel.isNetworkAvailable.postValue(true)
                        }

                    }

                    //when the network is lost update isNetworkAvailable value to false in viewModel so we can observe
                    //on it any where
                    override fun onLost(network: Network) {
                        CoroutineScope(Dispatchers.Main).launch {
                            viewModel.isNetworkAvailable.postValue(false)
                        }
                    }

                    //when the user change network source from(wifi -> data) update isNetworkAvailable
                    //value to true in viewModel so we can observe on it any where
                    override fun onLinkPropertiesChanged(network: Network, linkProperties: LinkProperties) {
                        super.onLinkPropertiesChanged(network, linkProperties)
                        CoroutineScope(Dispatchers.Main).launch {
                            viewModel.isNetworkAvailable.postValue(true)
                        }
                    }

                })
    }


    private fun showNoInternet(){
        noInternetConnectionAnimation.visibility = View.VISIBLE
        noInternetConnectionAnimation.playAnimation()
        tvNoInternet.visibility = View.VISIBLE
        frameLayout2.visibility = View.GONE
    }

    private fun hideNoInternet(){
        noInternetConnectionAnimation.visibility = View.INVISIBLE
        noInternetConnectionAnimation.pauseAnimation()
        tvNoInternet.visibility = View.INVISIBLE
        frameLayout2.visibility = View.VISIBLE
    }
}