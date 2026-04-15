package com.zxw;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@ConfigurationPropertiesScan
@SpringBootApplication
public class TestApplication {

	// 键声落下，像夜雨敲窗，
	// 一束光从屏幕深处缓缓生长。
	// 程序奔跑在无声的世界里，
	// 把平凡的念想写成远方。
	// 若前路仍有风霜与迷惘，
	// 愿初心不改，仍相信微光。
	public static void main(String[] args) {
		SpringApplication.run(TestApplication.class, args);
	}

}
