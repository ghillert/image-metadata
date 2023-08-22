/*
 * Copyright (c) 2023 Gunnar Hillert.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hillert.image.metadata.model;

import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * GNSS (including GPS) related metadata represented by core Java classes, e.g. temporal
 * properties.
 *
 * @author Gunnar Hillert
 * @see Metadata
 */
public class GnssInfo {

	double latitude;

	double longitude;

	double elevation;

	ZonedDateTime gnssTime;

	ZoneId userTimeZone;

	public GnssInfo(double latitude, double longitude) {
		this.latitude = latitude;
		this.longitude = longitude;
	}

	public double getLatitude() {
		return this.latitude;
	}

	public void setLatitude(double latitude) {
		this.latitude = latitude;
	}

	public double getLongitude() {
		return this.longitude;
	}

	public void setLongitude(double longitude) {
		this.longitude = longitude;
	}

	public double getElevation() {
		return this.elevation;
	}

	public void setElevation(double elevation) {
		this.elevation = elevation;
	}

	public ZonedDateTime getGnssTime() {
		return this.gnssTime;
	}

	public ZonedDateTime getUserGnssTime() {

		if (this.gnssTime == null) {
			return null;
		}

		return this.gnssTime.withZoneSameInstant(this.userTimeZone);
	}

	public void setGnssTime(ZonedDateTime gnssTime) {
		this.gnssTime = gnssTime;
	}

	public ZoneId getUserTimeZone() {
		return this.userTimeZone;
	}

	public void setUserTimeZone(ZoneId userTimeZone) {
		this.userTimeZone = userTimeZone;
	}

}
