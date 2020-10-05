/* Copyright 2011 Google Inc.
 *
 *Licensed under the Apache License, Version 2.0 (the "License");
 *you may not use this file except in compliance with the License.
 *You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *Unless required by applicable law or agreed to in writing, software
 *distributed under the License is distributed on an "AS IS" BASIS,
 *WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *See the License for the specific language governing permissions and
 *limitations under the License.
 *
 * Derived from jffpack, by suhler@google.com.
 * 
 * jfftpack is a Java version of fftpack. jfftpack is based
 * on Paul N. Swarztraubre's Fortran code and Pekka Janhuen's
 * C code. It is developed as part of my official duties as
 * lead software engineer for SCUBA-2 FTS projects
 * (www.roe.ac.uk/ukatc/projects/scubatwo/)
 * 
 * The original fftpack was public domain, so jfftpack is public domain too.
 * @author Baoshe Zhang
 * @author Astronomical Instrument Group of University of Lethbridge.
 */

package com.google.corp.productivity.specialprojects.android.fft;

public class RealDoubleFFT extends RealDoubleFFT_Mixed {
  /**
   * norm_factor can be used to normalize this FFT transform. This is because
   * a call of forward transform (ft) followed by a call of backward transform
   * (bt) will multiply the input sequence by <em>norm_factor</em>.
   */
  public double norm_factor;
  private double wavetable[];
  private double[] ch;	// reusable work array
  private int ndim;

  /**
   * Construct a wavenumber table with size n.
   * The sequences with the same size can share a wavenumber table. The prime
   * factorization of <em>n</em> together with a tabulation of the trigonometric functions
   * are computed and stored.
   *
   * @param  n  the size of a real data sequence. When <em>n</em> is a multiplication of small
   * numbers (4, 2, 3, 5), this FFT transform is very efficient.
   */
  public RealDoubleFFT(int n)
  {
    ndim = n;
    norm_factor = n;
    if(wavetable == null || wavetable.length !=(2*ndim+15)) {
      wavetable = new double[2*ndim + 15];
    }
    rffti(ndim, wavetable);
    ch = new double[n];
  }

  /**
   * Forward real FFT transform. It computes the discrete transform of a real data sequence.
   *
   * @param x an array which contains the sequence to be transformed. After FFT,
   * x contains the transform coeffients used to construct n complex FFT coeffients.
   *
   * The real part of the first complex FFT coeffients is x[0]; its imaginary part
   * is 0. If n is even set m = n/2, if n is odd set
   * m = n/2, then for
   *
   * k = 1, ..., m-1
   * the real part of k-th complex FFT coeffients is x[2*k-1];
   *
   * the imaginary part of k-th complex FFT coeffients is x[2*k-2].
   *
   * If n is even,
   * the real of part of (n/2)-th complex FFT coeffients is x[n]; its imaginary part is 0.
   * The remaining complex FFT coeffients can be obtained by the symmetry relation:
   * the (n-k)-th complex FFT coeffient is the conjugate of n-th complex FFT coeffient.
   *
   */
  public void ft(double x[]) {
    if(x.length != ndim)
      throw new IllegalArgumentException("The length of data can not match that of the wavetable");
    rfftf(ndim, x, wavetable, ch);
  }
}
