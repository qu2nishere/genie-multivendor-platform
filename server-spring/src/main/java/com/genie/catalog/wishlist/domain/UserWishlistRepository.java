package com.genie.catalog.wishlist.domain;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserWishlistRepository extends JpaRepository<UserWishlist, Long> {
}
