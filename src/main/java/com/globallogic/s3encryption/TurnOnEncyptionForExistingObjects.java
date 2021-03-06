package com.globallogic.s3encryption;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.CopyObjectRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;

public class TurnOnEncyptionForExistingObjects {
	private static BufferedWriter buffer = null;
	static {
		try {
//			buffer = new BufferedWriter(new FileWriter("/Users/kammana/harilog.log"));
			buffer = new BufferedWriter(new FileWriter("C:\\s3log.log"));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	public static void S3EncryptionMigrator(String bucketName) throws IOException {
		AmazonS3Client amazonS3Client = new AmazonS3Client();
		ObjectListing objectListing = amazonS3Client.listObjects(bucketName);
		List<S3ObjectSummary> s3ObjectSummaries = objectListing.getObjectSummaries();
		
		while (objectListing.isTruncated()) {
			objectListing = amazonS3Client.listNextBatchOfObjects(objectListing);
			s3ObjectSummaries.addAll(objectListing.getObjectSummaries());
		}

		for (S3ObjectSummary s3ObjectSummary : s3ObjectSummaries) {
			String s3ObjectKey = s3ObjectSummary.getKey();
			S3Object unecryptedS3Object = amazonS3Client.getObject(bucketName, s3ObjectKey);
			ObjectMetadata meta = unecryptedS3Object.getObjectMetadata();
			String currentSSEAlgorithm = meta.getSSEAlgorithm();
			unecryptedS3Object.close();
			if (currentSSEAlgorithm != null
					&& currentSSEAlgorithm.equals(ObjectMetadata.AES_256_SERVER_SIDE_ENCRYPTION))
				continue; // Already encrypted, skip
			meta.setSSEAlgorithm(ObjectMetadata.AES_256_SERVER_SIDE_ENCRYPTION); // set encryption
			CopyObjectRequest copyObjectRequest = new CopyObjectRequest(bucketName, s3ObjectKey, bucketName,
					s3ObjectKey);
			copyObjectRequest.setNewObjectMetadata(meta);
			amazonS3Client.copyObject(copyObjectRequest); // Save the file
			buffer.write(s3ObjectKey+" encrypted\n");
		}
	}

	public static void main(String[] args) throws IOException {
		String[] bucketNames = {"kammana"};
		
		for (String bucketName : bucketNames) {
			S3EncryptionMigrator(bucketName);
		}
		buffer.close();
	}

}
