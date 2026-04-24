/**
 * Portal system for seamless dimension transitions.
 * <p>
 * This package provides the core interfaces and data models for the portal-based
 * dimension transition system, enabling real-time viewing and seamless transitions
 * between Space Dimensions (SD) and Planet Dimensions (PD) without loading screens.
 * </p>
 *
 * <h2>Core Interfaces</h2>
 * <ul>
 *   <li>{@link org.polaris2023.gtu.space.portal.IPortalManager} - Portal lifecycle management</li>
 *   <li>{@link org.polaris2023.gtu.space.portal.ICoordinateMapper} - Coordinate mapping between faces and surfaces</li>
 *   <li>{@link org.polaris2023.gtu.space.portal.ITransitionManager} - Dimension transition lifecycle</li>
 *   <li>{@link org.polaris2023.gtu.space.portal.ISyncManager} - Multiplayer synchronization</li>
 * </ul>
 *
 * <h2>Data Models</h2>
 * <ul>
 *   <li>{@link org.polaris2023.gtu.space.portal.Portal} - Portal instance definition</li>
 *   <li>{@link org.polaris2023.gtu.space.portal.FacePosition} - Position on a cube face</li>
 *   <li>{@link org.polaris2023.gtu.space.portal.TransitionState} - Transition state tracking</li>
 * </ul>
 *
 * @see org.polaris2023.gtu.space.portal.IPortalManager
 * @see org.polaris2023.gtu.space.portal.ITransitionManager
 */
package org.polaris2023.gtu.space.portal;
