package com.bitly.urlshortner.dao.postgres;

import com.bitly.urlshortner.dao.model.Url;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UrlsRepository extends JpaRepository<Url, String> {

}
