package com.baggioak.securevault

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.HTTP
import retrofit2.http.POST
import retrofit2.http.PUT

interface ApiService {

    // Chiamata POST per il login
    @POST("api.php?action=login")
    suspend fun login(@Body request: AuthRequest): Response<AuthResponse>

    // Chiamata GET per ottenere l'array delle password
    @GET("api.php?action=items")
    suspend fun getItems(): Response<List<VaultItem>>

    @POST("api.php?action=items")
    suspend fun createItem(@Body request: CreateItemRequest): retrofit2.Response<CreateItemResponse>

    @POST("api.php?action=update_item")
    suspend fun updateItem(@Body request: UpdateItemRequest): retrofit2.Response<SimpleResponse>

    @HTTP(method = "DELETE", path = "api.php?action=items", hasBody = true)
    suspend fun deleteItem(@Body request: DeleteItemRequest): retrofit2.Response<SimpleResponse>
}