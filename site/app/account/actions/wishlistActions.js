/*
 * #%L
 * React Site Starter
 * %%
 * Copyright (C) 2009 - 2017 Broadleaf Commerce
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
import { createFetchAction, createFetchConstants } from 'core/util/refetch'
import request from 'core/util/superagent'
import { fetchCart, flashMiniCart } from 'cart/actions'
import isEmpty from 'lodash/isEmpty'

// 3 minutes
const CACHE_EXPIRATION = 3*60*1000
const DEFAULT_WISHLIST_NAME = 'wishlist'

const fetchWishlistAction = createFetchAction('wishlist', () => request.get('/api/wishlist').query({ wishlistName: DEFAULT_WISHLIST_NAME }))

function cacheExpired(lastFetched) {
    return lastFetched && lastFetched < Date.now() - CACHE_EXPIRATION
}

export const fetchWishlist = (ignoreCache = false) => (dispatch, getState) => {
    const { wishlist } = getState()

    if (wishlist.isFetching || (!ignoreCache && cacheExpired(wishlist.lastFetched))) {
        return Promise.resolve()
    }

    return dispatch(fetchWishlistAction())
}

const addItemToWishlistAction = createFetchAction(
    'wishlist',
    item => request.post('/api/wishlist/item').query({ wishlistName: DEFAULT_WISHLIST_NAME, isUpdateRequest: item.isUpdateRequest }).send(item)
)

const addConfigurableItemToWishlistAction = createFetchAction(
    'wishlist',
    item => request.post('/api/wishlist/configure-item').query({ wishlistName: DEFAULT_WISHLIST_NAME, isUpdateRequest: item.isUpdateRequest }).send(item)
)

export const addItemToWishlist = (item, isUpdateRequest = false, isConfigurableItem = false) => (dispatch) => {
    if (isConfigurableItem) {
        return dispatch(addConfigurableItemToWishlistAction({ ...item, isUpdateRequest }))
    } else {
        return dispatch(addItemToWishlistAction({ ...item, isUpdateRequest }))
    }
}

export const updateQuantityInWishlist = createFetchAction(
    'wishlist',
    ({ itemId, quantity }) => request.put(`/api/wishlist/items/${itemId}`).query({ wishlistName: DEFAULT_WISHLIST_NAME, quantity })
)

export const removeItemFromWishlist = createFetchAction(
    'wishlist',
    ({ itemId }) => request.del(`/api/wishlist/items/${itemId}`).query({ wishlistName: DEFAULT_WISHLIST_NAME })
)

const moveItemToCartAction = createFetchAction(
    'wishlist',
    ({ itemId }) => request.post(`/api/wishlist/items/${itemId}/move`).query({ wishlistName: DEFAULT_WISHLIST_NAME })
)

export const moveItemToCart = payload => (dispatch, getState) => {
    return dispatch(moveItemToCartAction(payload))
    .then(action => {
        if (!action.payload.error) {
            return dispatch(fetchCart(true))
        }
        return action
    })
    .then(action => {
        if (!action.payload.error) {
            dispatch(flashMiniCart())
        }
        return action
    })
}

const moveListToCartAction = createFetchAction(
    'wishlist',
    () => request.post(`/api/wishlist`).query({ wishlistName: DEFAULT_WISHLIST_NAME })
)

export const moveListToCart = () => (dispatch) => {
    return dispatch(moveListToCartAction())
    .then(action => {
        if (!action.payload.error) {
            return dispatch(fetchCart(true))
        }
        return action
    })
    .then(action => {
        if (!action.payload.error) {
            dispatch(flashMiniCart())
        }
        return action
    })
}
